// Family group by QR (no accounts) + spelling-aware family search. Same rules on Android (shared/fixtures/hybrid.json).
// The QR carries the family id and the whole small group the phone knows, so news travels phone to phone with no signal.
import { ageBand, familySearch, normalizeName, type PublicRec } from './dedupe';
import { levenshtein } from './drugMatcher';

export type Member = { id: string; name: string; shelter: string; status: 'safe' | 'need_help'; at: number };

const FAM = /^f-[0-9a-f]{20}$/;
export const newFamilyId = () => 'f-' + [...crypto.getRandomValues(new Uint8Array(10))].map(b => b.toString(16).padStart(2, '0')).join('');
const clean = (s: string) => s.replace(/[|,:]/g, '').trim().slice(0, 60);

/** First member = the person showing the QR. */
export const encodeFamilyQr = (fam: string, members: Member[]) =>
  `NMF2:${fam}|` + members.slice(0, 8).map(m => [clean(m.id), clean(m.shelter), m.status, m.at, clean(m.name)].join(',')).join('|');

/** Trust boundary: anything scanned is checked field by field; one bad entry rejects the code. */
export function decodeFamilyQr(text: string): { fam: string; members: Member[] } | null {
  const [head, ...rows] = text.trim().split('|');
  const fam = /^NMF2:(.+)$/.exec(head)?.[1];
  if (!fam || !FAM.test(fam) || !rows.length || rows.length > 12) return null;
  const members: Member[] = [];
  for (const r of rows) {
    const m = /^([^,]{1,40}),([^,]{1,40}),(safe|need_help),(\d{1,15}),(.{1,60})$/.exec(r);
    if (!m) return null;
    members.push({ id: m[1], shelter: m[2], status: m[3] as Member['status'], at: Number(m[4]), name: m[5] });
  }
  return { fam, members };
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

/** Offline spelling rules (abbreviations both ways + joined first two words). The cloud adds more when online. */
export function offlineVariants(name: string, rules: Record<string, string[]>): string[] {
  const toks = name.trim().split(/\s+/);
  const out: string[] = [];
  toks.forEach((t, i) => {
    const key = Object.keys(rules).find(k => k.toLowerCase() === t.toLowerCase());
    const back = Object.entries(rules).find(([, alts]) => alts.some(a => a.toLowerCase() === t.toLowerCase()))?.[0];
    for (const alt of [...(key ? rules[key] : []), ...(back ? [back] : [])]) out.push([...toks.slice(0, i), alt, ...toks.slice(i + 1)].join(' '));
  });
  if (toks.length >= 2) out.push(toks[0] + toks[1] + (toks.length > 2 ? ' ' + toks.slice(2).join(' ') : ''));
  return [...new Set(out)].filter(v => v.toLowerCase() !== name.trim().toLowerCase());
}

/** Exact name first, then other spellings. An age (if known) drops people more than 10 years off. A person always confirms. */
export function searchVariants(name: string, variants: string[], age: number | null, records: PublicRec[]) {
  const near = (r: PublicRec) => age == null || r.age == null || Math.abs(r.age - age) <= 10;
  const pool = records.filter(near);
  const seen = new Set<string>();
  const hit = (r: PublicRec, match: 'exact' | 'spelling') => {
    const k = `${r.name}|${r.shelterId}`;
    if (seen.has(k)) return [];
    seen.add(k);
    return [{ name: r.name, ageBand: ageBand(r.age), shelterId: r.shelterId, status: r.status, match }];
  };
  const exact = pool.filter(r => familySearch(name, [r]).length).flatMap(r => hit(r, 'exact'));
  // Offline safety net: every word of the name matches a word in the record with at most one letter different (words of 4+ letters).
  const q = normalizeName(name).split(' ').filter(Boolean);
  const close = (r: PublicRec) => { const w = normalizeName(r.name).split(' '); return q.length > 0 && q.every(t => w.some(x => x === t || (t.length >= 4 && levenshtein(t, x) <= 1))); };
  const spelling = pool.filter(r => !r.hidden && (variants.some(v => familySearch(v, [r]).length) || close(r))).flatMap(r => hit(r, 'spelling'));
  return [...exact, ...spelling];
}

const RELATION = /\b(my|saya|aku|nenek|kakek|ibu|bapak|pak|bu|ayah|mama|papa|anak|adik|kakak|suami|istri|om|tante|paman|bibi|grandmother|grandfather|grandma|grandpa|mother|father|son|daughter|brother|sister|husband|wife|uncle|aunt|named|bernama|namanya)\b/gi;
/** No device model: pull name, age and place out of "my grandmother Nurhayati, about 80, from Sukajadi" with plain rules. */
export function parseLookFor(text: string) {
  const age = Number(/(\d{1,3})\s*(an|s)?\b/.exec(text)?.[1]) || null;
  const place = /\b(?:from|dari)\s+([\p{L} ]+?)(?:[,.]|$)/iu.exec(text)?.[1]?.trim() || null;
  const head = text.split(/[,.]|\b(?:about|around|sekitar|kira-kira|umur|usia|aged?|from|dari)\b|\d/i)[0];
  const name = head.replace(RELATION, ' ').replace(/\s+/g, ' ').trim() || text.trim();
  return { name, age, place };
}
