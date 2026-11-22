// F1: roster from a photo of a handwritten list. Read on the device; staff confirm every name + flag.
import { mapRosterRows, type RosterRow } from '../domain/rosterMapping';
import type { ImageInput } from '../ports';
import { type Deps, uid } from './deps';
import type { Person } from './state';

export function createRoster(d: Deps, enqueue: (type: string, payload: unknown, path?: string) => unknown) {
  async function scan(image: ImageInput, staffApprovedCloud = false) {
    const r = await d.router.run<{ rows: RosterRow[] }>('roster_read', { image }, { staffApprovedCloud });
    if ((r.status === 'ok' || r.status === 'draft') && r.data) {
      const drafts: Person[] = mapRosterRows(r.data.rows, r.source!, d.rules).map(p => ({
        ...p, id: uid(), flags: [], status: 'safe', needsReview: p.needsReview || r.status === 'draft',
      }));
      d.store.set(s => ({ ...s, people: [...s.people, ...drafts] }));
    }
    // Batch mode (6.10): the photo waits on this device (memory only, never synced) until signal or "Process now".
    if (r.status === 'queued') d.store.set(s => ({ ...s, batch: [...s.batch, { id: uid(), kind: 'roster', image, approved: staffApprovedCloud }] }));
    return r;
  }

  async function processBatch() {
    for (const b of d.store.get().batch.filter(x => x.kind === 'roster')) {
      d.store.set(s => ({ ...s, batch: s.batch.filter(x => x.id !== b.id) }));
      const r = await scan(b.image, b.approved); // re-queues itself if still offline
      // A failed cloud read must not lose the photo: back into the batch for the next signal or "Process now".
      if (r.status === 'error' || r.status === 'invalid') d.store.set(s => ({ ...s, batch: [...s.batch, b] }));
    }
  }

  function confirm(id: string, patch: Partial<Pick<Person, 'nameAsWritten' | 'age' | 'flags' | 'status' | 'hidden'>>) {
    d.store.set(s => ({ ...s, people: s.people.map(p => (p.id === id ? { ...p, ...patch, flags: patch.flags ?? p.suggestedFlags, confirmedBy: 'staff', needsReview: false } : p)) }));
    const p = d.store.get().people.find(x => x.id === id)!;
    const { shelter } = d.store.get();
    // Public family search index: minimal fields only (D6). Age is stored as its decade.
    enqueue('family_update', { name: p.nameAsWritten, age: p.age == null ? null : Math.floor(p.age / 10) * 10, shelterId: shelter.id, status: p.status, hidden: !!p.hidden }, `public/${shelter.id}_${p.id}`);
    enqueue('roster_merge', { ...p }, `shelters/${shelter.id}/people/${p.id}`);
  }

  const addPerson = (name: string, age: number | null) => {
    const id = uid();
    d.store.set(s => ({ ...s, people: [...s.people, { id, nameAsWritten: name, age, sex: null, flags: [], suggestedFlags: [], notes: null, status: 'safe', needsReview: true, inferredBy: 'staff', confirmedBy: null }] }));
    return id;
  };

  return { scan, confirm, addPerson, processBatch };
}
