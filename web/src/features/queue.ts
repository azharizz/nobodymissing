// Outbox (F11): jobs wait locally, sync in priority order when signal returns. QR courier for store-and-forward.
import { chunk, join } from '../domain/courier';
import { orderJobs, progress, transition } from '../domain/queue';
import { type Deps, uid } from './deps';
import type { Job } from './state';

const COLLECTION: Record<string, string> = {
  cant_miss_alert: 'alerts', transfer_request: 'requests', med_shortage: 'requests', family_update: 'public',
  roster_merge: 'roster', field_report: 'fieldReports', other: 'announcements',
};

export function createQueue(d: Deps, reRun: (j: Job) => Promise<boolean>) {
  const update = (id: string, fn: (j: Job) => Job) => d.store.set(s => ({ ...s, jobs: s.jobs.map(j => (j.id === id ? fn(j) : j)) }));
  const step = (id: string, ev: Parameters<typeof transition>[1]) => update(id, j => ({ ...j, ...transition(j, ev) }));

  // A job cut off mid-send (page closed, phone died) is marked failed so the next flush retries it instead of leaving it stuck.
  if (d.store.get().jobs.some(j => j.state === 'processing' || j.state === 'sending'))
    d.store.set(s => ({ ...s, jobs: s.jobs.map(j => (j.state === 'processing' || j.state === 'sending' ? { ...j, ...transition(j, 'fail') } : j)) }));

  function enqueue(type: string, payload: unknown, path?: string | null): Job {
    const id = uid();
    const job: Job = { id, type, payload, path: path === undefined ? `${COLLECTION[type] ?? 'other'}/${id}` : path, createdAt: d.clock.now(), state: 'waiting', retries: 0 };
    d.store.set(s => ({ ...s, jobs: [...s.jobs, job] }));
    if (d.store.get().signal === 'back') queueMicrotask(() => void flush()); // signal now: send now, don't wait for the next scout tick
    return job;
  }
  // Firestore waits for the server ack; with a weak link that can take forever. One stuck job must not block the pickup behind it.
  const JOB_TIMEOUT_MS = 30_000;
  const limit = <T,>(p: Promise<T>) => Promise.race([p, new Promise<never>((_, rej) => setTimeout(() => rej(new Error('job timeout')), JOB_TIMEOUT_MS))]);

  let flushing = false;
  async function flush() {
    if (flushing) return;
    flushing = true;
    try {
      const pending = orderJobs(d.store.get().jobs.filter(j => j.state === 'waiting' || j.state === 'failed'), d.rules.queuePriority);
      for (const j of pending) {
        if (j.state === 'failed') step(j.id, 'retry');
        step(j.id, 'start');
        try {
          if (j.path) { step(j.id, 'send'); await limit(d.sync.put(j.path, j.payload as object)); }
          else { if (!(await limit(reRun(j)))) { step(j.id, 'fail'); continue; } step(j.id, 'send'); }
          step(j.id, 'ack');
        } catch { step(j.id, 'fail'); }
      }
    } finally { flushing = false; }
  }

  /** Small text first (6.10): only structured JSON travels, never photos. */
  const exportChunks = (size = 700) => chunk(JSON.stringify(d.store.get().jobs.filter(j => j.state !== 'sent')), size);

  /** Courier import is a trust boundary: accept only well-formed jobs, skip ones we already have. */
  function importChunks(chunks: string[]): number {
    const raw = join(chunks);
    if (!raw) return 0;
    let list: unknown;
    try { list = JSON.parse(raw); } catch { return 0; }
    if (!Array.isArray(list)) return 0;
    const have = new Set(d.store.get().jobs.map(j => j.id));
    const ok = list.filter((j): j is Job => !!j && typeof j.id === 'string' && typeof j.type === 'string' && (j.path === null || typeof j.path === 'string') && !have.has(j.id))
      .map(j => ({ ...j, state: 'waiting' as const, retries: 0 }));
    d.store.set(s => ({ ...s, jobs: [...s.jobs, ...ok] }));
    return ok.length;
  }

  return { enqueue, flush, exportChunks, importChunks, progress: () => progress(d.store.get().jobs.map(j => j.state)) };
}
