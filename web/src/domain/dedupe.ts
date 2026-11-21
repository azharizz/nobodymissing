// Cross-shelter roster merge + public family search (F3, D6). Deterministic keys, no LLM.
import { levenshtein } from './drugMatcher';
import { cmp } from './cmp';

const HONORIFICS = new Set(['pak', 'bapak', 'bpk', 'bu', 'ibu', 'mr', 'mrs', 'ms', 'san', 'sdr', 'sdri']);
const OLD_SPELLING: [RegExp, string][] = [[/oe/g, 'u'], [/dj/g, 'j'], [/tj/g, 'c'], [/sj/g, 'sy'], [/nj/g, 'ny']];

export function normalizeName(name: string): string {
  let s = name.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z]+/g, ' ');
  for (const [re, to] of OLD_SPELLING) s = s.replace(re, to);
  return s.split(' ').filter(t => t && !HONORIFICS.has(t)).join(' ');
}

type Rec = { id: string; name: string; age: number | null };
const sortedKey = (n: string) => normalizeName(n).split(' ').sort().join(' ');

export function samePerson(a: Rec, b: Rec): boolean {
  if (a.age != null && b.age != null && Math.abs(a.age - b.age) > 2) return false;
  const ka = sortedKey(a.name), kb = sortedKey(b.name);
  if (ka === kb) return true;
  // ponytail: O(n^2) pairwise scan, fine for a few thousand rows; block by first letter if a region grows past that
  return ka.replace(/ /g, '').length >= 5 && kb.replace(/ /g, '').length >= 5 && levenshtein(ka, kb) <= 1;
}

export function groupDuplicates(records: Rec[]): string[][] {
  const parent = records.map((_, i) => i);
  const find = (i: number): number => (parent[i] === i ? i : (parent[i] = find(parent[i])));
  for (let i = 0; i < records.length; i++)
    for (let j = i + 1; j < records.length; j++)
      if (samePerson(records[i], records[j])) parent[find(j)] = find(i);
  const groups = new Map<number, string[]>();
  records.forEach((r, i) => { const k = find(i); groups.set(k, [...(groups.get(k) ?? []), r.id]); });
  return [...groups.values()].map(g => g.sort(cmp)).sort((a, b) => cmp(a[0], b[0]));
}

export type PublicRec = Rec & { shelterId: string; status: string; hidden?: boolean };
export const ageBand = (age: number | null) => (age == null ? 'unknown' : `${Math.floor(age / 10) * 10}-${Math.floor(age / 10) * 10 + 9}`);

/** Public search returns minimal fields only (D6): name, age band, shelter, status. */
export function familySearch(query: string, records: PublicRec[]) {
  const q = normalizeName(query).split(' ').filter(Boolean);
  if (!q.length) return [];
  return records
    .filter(r => !r.hidden && q.every(t => normalizeName(r.name).split(' ').includes(t)))
    .map(r => ({ name: r.name, ageBand: ageBand(r.age), shelterId: r.shelterId, status: r.status }));
}
