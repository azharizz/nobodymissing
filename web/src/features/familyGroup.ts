// Family group by QR. Offline: a QR carries the whole group the phone knows, so news hops phone to phone.
// Online: each phone shares everyone it knows into every family id it has seen (gossip), so groups that met
// through different relatives join up and nobody is stranded in an old group.
import { decodeFamilyQr, encodeFamilyQr, mergeMembers, newFamilyId, type Member } from '../domain/family';
import { type Deps, online, uid } from './deps';

export function createFamilyGroup(d: Deps) {
  const get = () => d.store.get().familyGroup;
  type G = NonNullable<ReturnType<typeof get>>;
  const save = (g: G) => d.store.set(s => ({ ...s, familyGroup: g }));

  /** Set my name and status (creates the group on first use). */
  function setMe(name: string, status: Member['status']) {
    const g = get(), fam = g?.fam ?? newFamilyId();
    const me: Member = { id: g?.me.id ?? uid().slice(0, 12), name: name.trim().slice(0, 60), shelter: d.store.get().shelter.id, status, at: d.clock.now() };
    save({ fam, fams: g?.fams ?? [fam], me, members: g?.members ?? [], via: g?.via ?? {} });
    void sync();
  }
  const myQr = () => { const g = get(); return g ? encodeFamilyQr(g.fam, [g.me, ...g.members]) : null; };

  /** Scan a relative's QR: learn everyone on it. Two groups that meet converge on the smaller id; both ids stay synced. */
  function scan(text: string): Member[] | null {
    const q = decodeFamilyQr(text), g = get();
    if (!q || !g) return null;
    const theirs = q.members.filter(m => m.id !== g.me.id);
    if (!theirs.length) return null;
    const via = { ...g.via };
    for (const t of theirs) if (!g.members.some(m => m.id === t.id && m.at >= t.at)) via[t.id] = 'qr';
    const fams = [...new Set([...g.fams, q.fam])];
    save({ ...g, fam: [g.fam, q.fam].sort()[0], fams, members: mergeMembers(g.members, theirs), via });
    void sync();
    return theirs;
  }

  async function sync() {
    const g = get();
    if (!g || !online(d)) return;
    try {
      let members = g.members;
      const via = { ...g.via };
      const remoteByFam: Record<string, Member[]> = {};
      for (const fam of g.fams) {
        const rows = await d.sync.list(`families/${fam}/members`);
        const remote: Member[] = rows.filter(r => typeof r.name === 'string')
          .map(r => ({ id: String(r.id), name: String(r.name), shelter: String(r.shelter), status: r.status === 'need_help' ? 'need_help' : 'safe', at: Number(r.at) || 0 }));
        remoteByFam[fam] = remote;
        const others = remote.filter(r => r.id !== g.me.id);
        for (const r of others) if (!members.some(m => m.id === r.id && m.at >= r.at)) via[r.id] = 'online';
        members = mergeMembers(members, others);
      }
      // Gossip: write anyone a family id is missing or has an older copy of.
      for (const fam of g.fams) for (const m of [g.me, ...members]) {
        const have = remoteByFam[fam].find(r => r.id === m.id);
        if (!have || have.at < m.at) { const { id, ...rec } = m; await d.sync.put(`families/${fam}/members/${id}`, rec); }
      }
      save({ ...get()!, members, via });
    } catch { /* offline again: the QR still works */ }
  }

  return { setMe, myQr, scan, sync };
}
