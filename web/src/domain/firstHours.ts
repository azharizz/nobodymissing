// The first hours: warning -> the way -> pickup -> check-in. Safety rules are code, not prompts.
// Same rules on Android (shared/fixtures/hybrid.json). The AI only fills in; words that matter can only raise urgency.
const has = (text: string, words: string[]) => { const t = ` ${text.toLowerCase()} `; return words.some(w => t.includes(w.toLowerCase())); };
const LEVELS = ['advice', 'prepare', 'go_now'] as const;
export type Level = (typeof LEVELS)[number];
export type Warning = { hazard: string; level: Level; plain: string; action: string | null; by: 'device' | 'cloud' | 'keywords' };

/** Official warning -> plain words. "evakuasi segera" in the original always means go now, whatever the model says. */
export function plainWarning(text: string, ai: { hazard: string; level: Level; plain: string; action?: string | null } | null, by: 'device' | 'cloud',
  rules: { goNowWords: string[]; hazardWords: Record<string, string[]> }): Warning {
  const goNow = has(text, rules.goNowWords);
  const hazard = Object.entries(rules.hazardWords).find(([, w]) => has(text, w))?.[0] ?? 'other';
  if (ai) {
    const level = goNow ? 'go_now' : ai.level;
    return { hazard: ai.hazard === 'other' ? hazard : ai.hazard, level, plain: ai.plain, action: ai.action ?? null, by };
  }
  const first = /^.*?[.!?](\s|$)/.exec(text.trim())?.[0].trim() ?? text.trim();
  return { hazard, level: goNow ? 'go_now' : 'prepare', plain: first.slice(0, 160), action: null, by: 'keywords' };
}

export type Checkin = { name: string; age: number | null; flags: string[] };
/** Volunteer's sentence at the door -> name, age, flags (keywords). */
export function parseCheckin(text: string, flagWords: Record<string, string[]>): Checkin {
  const name = text.split(/[,;]|\d/)[0].trim().slice(0, 60);
  const age = Number(/\b(\d{1,3})\b/.exec(text)?.[1]);
  return { name, age: age > 0 && age <= 120 ? age : null, flags: Object.keys(flagWords).filter(f => has(text, flagWords[f])) };
}
/** Flags the words clearly say are never dropped, even if the model forgot them. */
export function mergeCheckin(ai: Checkin | null, text: string, flagWords: Record<string, string[]>) {
  const r = parseCheckin(text, flagWords);
  if (!ai) return { ...r, by: 'keywords' as const };
  return { name: ai.name || r.name, age: ai.age ?? r.age, flags: [...new Set([...ai.flags, ...r.flags])], by: 'device' as const };
}

export type Pickup = { who: string; mobility: number; needs: string[]; people: number };
/** "My mother can't walk, needs insulin" -> pickup request (keywords). */
export function parsePickup(text: string, needWords: Record<string, string[]>, mobilityWords: Record<string, string[]>): Pickup {
  const mobility = Math.max(0, ...Object.entries(mobilityWords).filter(([, w]) => has(text, w)).map(([k]) => Number(k)));
  return { who: text.split(/[,;.]/)[0].trim().slice(0, 40), mobility, needs: Object.keys(needWords).filter(n => has(text, needWords[n])), people: 1 };
}
/** Mobility and needs can only go up. */
export function mergePickup(ai: Pickup | null, text: string, needWords: Record<string, string[]>, mobilityWords: Record<string, string[]>) {
  const r = parsePickup(text, needWords, mobilityWords);
  if (!ai) return { ...r, by: 'keywords' as const };
  return { who: ai.who || r.who, mobility: Math.max(ai.mobility, r.mobility), needs: [...new Set([...ai.needs, ...r.needs])], people: Math.max(1, ai.people), by: 'device' as const };
}
/** A pickup becomes a Field Check entry ranked by the same priority rules. */
export const pickupPriority = (p: Pickup) => ({
  mobility: Math.min(3, p.mobility) as 0 | 1 | 2 | 3, cantMiss: p.needs.some(n => ['insulin', 'dialysis', 'oxygen'].includes(n)),
  dailyMeds: p.needs.includes('daily_meds'), hearing: p.needs.includes('deaf'), dementia: p.needs.includes('dementia'),
});

const FLAGS = ['hard_of_hearing', 'deaf', 'needs_meds', 'wheelchair', 'cant_walk', 'lives_alone', 'foreign_language', 'infant', 'pregnant', 'elderly', 'dementia'];
const NEEDS = ['insulin', 'dialysis', 'oxygen', 'daily_meds', 'baby', 'pregnant', 'wheelchair', 'bedridden', 'deaf', 'blind', 'dementia', 'other'];
const clean = (s: string) => s.replace(/\|/g, '').trim().slice(0, 60);

/** Check-in card QR: name, age, needs the person chose themselves. */
export const encodeCheckinQr = (c: Checkin) => `NMC1|${clean(c.name)}|${c.age ?? ''}|${c.flags.filter(f => FLAGS.includes(f)).join(',')}`;
export function decodeCheckinQr(text: string): Checkin | null {
  const m = /^NMC1\|([^|]{1,60})\|(\d{0,3})\|([a-z_,]*)$/.exec(text.trim());
  if (!m) return null;
  const age = m[2] ? Number(m[2]) : null, flags = m[3] ? m[3].split(',') : [];
  if ((age != null && age > 120) || flags.some(f => !FLAGS.includes(f))) return null;
  return { name: m[1].trim(), age, flags };
}

export type PickupRecord = Pickup & { id: string; lat: number; lng: number; at: number };
/** Pickup QR: carried to any volunteer by hand when there is no signal at all. */
export const encodePickupQr = (p: PickupRecord) => `NMP1|${clean(p.id)}|${p.lat}|${p.lng}|${p.mobility}|${p.needs.join(',')}|${p.people}|${p.at}|${clean(p.who)}`;
export function decodePickupQr(text: string): PickupRecord | null {
  const m = /^NMP1\|([^|]{1,40})\|(-?\d+(?:\.\d+)?)\|(-?\d+(?:\.\d+)?)\|([0-3])\|([a-z_,]*)\|(\d{1,2})\|(\d{1,15})\|([^|]{1,40})$/.exec(text.trim());
  if (!m) return null;
  const lat = Number(m[2]), lng = Number(m[3]), needs = m[5] ? m[5].split(',') : [];
  if (Math.abs(lat) > 90 || Math.abs(lng) > 180 || needs.some(n => !NEEDS.includes(n))) return null;
  return { id: m[1], lat, lng, mobility: Number(m[4]), needs, people: Number(m[6]), at: Number(m[7]), who: m[8] };
}
