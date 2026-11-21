// Deterministic drug-name matcher (6.9). Not AI. Never auto-accepts: a human always confirms.
import { cmp } from './cmp';
export type Drug = { generic: string; brands: string[]; strengths: string[]; form: string; class: string };
export type DrugMatch = {
  status: 'matched' | 'candidates' | 'unknown'; generic: string | null; candidates: string[];
  lookAlikes: string[]; strengthKnown: boolean | null; needsConfirm: true;
};

const STRENGTH_RE = /(\d+(?:\.\d+)?)\s*(mg|mcg|g|ml|units\/ml|iu)\b/i;

export const normStrength = (s: string) => s.toLowerCase().replace(/\s+/g, '').replace(/[^a-z0-9./]/g, '');
const norm = (s: string) => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z ]/g, ' ').replace(/\s+/g, ' ').trim();

export function levenshtein(a: string, b: string): number {
  const row = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    let prev = row[0]; row[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const tmp = row[j];
      row[j] = Math.min(row[j] + 1, row[j - 1] + 1, prev + (a[i - 1] === b[j - 1] ? 0 : 1));
      prev = tmp;
    }
  }
  return row[b.length];
}

export function matchDrug(text: string, strength: string | null, list: Drug[]): DrugMatch {
  let str = strength;
  const m = text.match(STRENGTH_RE);
  if (!str && m) str = m[0];
  const name = norm(text.replace(STRENGTH_RE, ''));
  const exact = list.find(d => d.generic === name || d.brands.includes(name));
  const base = { candidates: [] as string[], lookAlikes: [] as string[], needsConfirm: true as const };
  if (exact) {
    const lookAlikes = list.filter(d => d !== exact && levenshtein(d.generic, exact.generic) <= 3).map(d => d.generic).sort();
    const strengthKnown = str ? exact.strengths.some(s => normStrength(s) === normStrength(str!)) : null;
    return { ...base, status: 'matched', generic: exact.generic, lookAlikes, strengthKnown };
  }
  const maxDist = Math.max(1, Math.floor(name.length / 4));
  const candidates = list
    .flatMap(d => [d.generic, ...d.brands].map(n => ({ g: d.generic, d: levenshtein(n, name) })))
    .filter(x => x.d <= maxDist)
    .sort((a, b) => a.d - b.d || cmp(a.g, b.g))
    .map(x => x.g)
    .filter((g, i, arr) => arr.indexOf(g) === i)
    .slice(0, 3);
  return { ...base, status: candidates.length ? 'candidates' : 'unknown', generic: null, candidates, strengthKnown: null };
}

type Med = { generic: string; strength: string; form: string };
/** Alternative-medicine safety levels (6.9): the app never picks a different medicine. */
export function substitutionLevel(prescribed: Med, available: Med): 'same' | 'different_strength_or_form' | 'ask_doctor' {
  if (prescribed.generic !== available.generic) return 'ask_doctor';
  return normStrength(prescribed.strength) === normStrength(available.strength) && prescribed.form === available.form
    ? 'same' : 'different_strength_or_form';
}
