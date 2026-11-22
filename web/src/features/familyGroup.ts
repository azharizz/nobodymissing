// Family group by QR. Offline: scanning each other's QR carries the latest status both ways.
// Online: my record goes to families/{familyId}/members/{myId} through the outbox, and the group is read back.
import { decodeFamilyQr, encodeFamilyQr, mergeMembers, newFamilyId, type Member } from '../domain/family';
import { type Deps, online, uid } from './deps';

export function createFamilyGroup(d: Deps, enqueue: (type: string, payload: unknown, path?: string | null) => unknown) {
  const get = () => d.store.get().familyGroup;
  const save = (g: NonNullable<ReturnType<typeof get>>) => d.store.set(s => ({ ...s, familyGroup: g }));
  const publish = () => { const g = get(); if (g?.me) { const { id, ...rec } = g.me; enqueue('family_member', rec, `families/${g.fam}/members/${id}`); } };

  /** Set my name and status (creates the group on first use). */
  function setMe(name: string, status: Member['status']) {
    const g = get(), sh = d.store.get().shelter.id;
    const me: Member = { id: g?.me.id ?? uid().slice(0, 12), name: name.trim().slice(0, 60), shelter: sh, status, at: d.clock.now() };
    save({ fam: g?.fam ?? newFamilyId(), me, members: g?.members ?? [], via: g?.via ?? {} });
    publish();
  }
  const myQr = () => { const g = get(); return g ? encodeFamilyQr(g.fam, g.me) : null; };

  /** Scan a family member's QR: join their group (if I have none yet, or merge into theirs) and keep their latest status. */
  function scan(text: string): Member | null {
    const q = decodeFamilyQr(text), g = get();
    if (!q || !g || q.member.id === g.me.id) return null;
    save({ ...g, fam: q.fam, members: mergeMembers(g.members, [q.member]), via: { ...g.via, [q.member.id]: 'qr' } });
    if (q.fam !== g.fam) publish();
    return q.member;
  }

  async function sync() {
    const g = get();
    if (!g || !online(d)) return;
    try {
      const rows = await d.sync.list(`families/${g.fam}/members`);
      const remote: Member[] = rows.filter(r => r.id !== g.me.id && typeof r.name === 'string')
        .map(r => ({ id: String(r.id), name: String(r.name), shelter: String(r.shelter), status: r.status === 'need_help' ? 'need_help' : 'safe', at: Number(r.at) || 0 }));
      const via = { ...g.via };
      for (const r of remote) if (!g.members.some(m => m.id === r.id && m.at >= r.at)) via[r.id] = 'online';
      save({ ...g, members: mergeMembers(g.members, remote), via });
    } catch { /* offline again: QR still works */ }
  }

  return { setMe, myQr, scan, sync };
}
