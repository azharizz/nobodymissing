// Family group by QR (no accounts): each phone shows a QR with the family id + its own latest status.
// Scanning exchanges status offline; online, members sync through families/{familyId}. The family id is the only key.
export type Member = { id: string; name: string; shelter: string; status: 'safe' | 'need_help'; at: number };

const FAM = /^f-[0-9a-f]{20}$/;
export const newFamilyId = () => 'f-' + [...crypto.getRandomValues(new Uint8Array(10))].map(b => b.toString(16).padStart(2, '0')).join('');

export const encodeFamilyQr = (fam: string, me: Member) => `NMF1:${fam}:${me.id}:${me.shelter}:${me.status}:${me.at}:${me.name}`;

/** Trust boundary: anything scanned is checked field by field. */
export function decodeFamilyQr(text: string): { fam: string; member: Member } | null {
  const m = /^NMF1:([^:]+):([^:]+):([^:]+):(safe|need_help):(\d+):(.{1,60})$/.exec(text.trim());
  if (!m || !FAM.test(m[1])) return null;
  return { fam: m[1], member: { id: m[2], shelter: m[3], status: m[4] as Member['status'], at: Number(m[5]), name: m[6] } };
}

/** Newer update wins per member; nobody is dropped. */
export function mergeMembers(mine: Member[], theirs: Member[]): Member[] {
  const out = mine.map(m => ({ ...m }));
  for (const t of theirs) {
    const i = out.findIndex(m => m.id === t.id);
    if (i < 0) out.push({ ...t }); else if (t.at > out[i].at) out[i] = { ...t };
  }
  return out;
}
