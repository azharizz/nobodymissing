// Outbox job queue: state machine + priority order (6.10). Every job is idempotent and has a visible state.
export type JobState = 'waiting' | 'processing' | 'sending' | 'sent' | 'failed';
export type JobEvent = 'start' | 'send' | 'ack' | 'fail' | 'retry';
export type JobStatus = { state: JobState; retries: number };

const NEXT: Record<JobEvent, [JobState[], JobState]> = {
  start: [['waiting'], 'processing'],
  send: [['processing'], 'sending'],
  ack: [['sending'], 'sent'],
  fail: [['processing', 'sending'], 'failed'],
  retry: [['failed'], 'waiting'],
};

export function transition(j: JobStatus, ev: JobEvent): JobStatus {
  const [from, to] = NEXT[ev];
  if (!from.includes(j.state)) throw new Error(`invalid ${ev} from ${j.state}`);
  return { state: to, retries: j.retries + (ev === 'retry' ? 1 : 0) };
}

export const applyEvents = (events: JobEvent[], start: JobStatus = { state: 'waiting', retries: 0 }) => events.reduce(transition, start);

export function orderJobs<T extends { type: string; createdAt: number }>(jobs: T[], prio: Record<string, number>): T[] {
  const p = (t: string) => prio[t] ?? prio.other;
  return [...jobs].sort((a, b) => p(a.type) - p(b.type) || a.createdAt - b.createdAt);
}

export function progress(states: JobState[]) {
  const sent = states.filter(s => s === 'sent').length;
  return { sent, total: states.length, waiting: states.length - sent };
}
