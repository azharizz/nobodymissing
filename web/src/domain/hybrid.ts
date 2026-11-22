// Safety contracts for the three hybrid features (medicine check, find it open, Ask Kit).
// Pure code, same fixtures on Android (shared/fixtures/hybrid.json). The AI never decides these.
import { matchDrug, type Drug } from './drugMatcher';

/** Privacy split: the cloud request is built from a whitelist, so names, beds and photos cannot get in. */
export function medCheckInput(meds: { nameAsWritten: string; strength: string | null; generic?: string | null }[]) {
  return { meds: meds.map(m => ({ name: m.generic || m.nameAsWritten, strength: m.strength ?? null })) };
}

/** Coarse location: rounded before it leaves the device (2 decimals is about 1 km). */
export function coarseLocation(lat: number, lng: number, decimals: number) {
  const f = 10 ** decimals, r = (x: number) => Math.round(x * f) / f;
  return { lat: r(lat), lng: r(lng) };
}

export type CrossStatus = 'agree' | 'mismatch' | 'cloud_only' | 'offline_only' | 'unknown';
/** The cloud's generic name is checked against the offline drug list. A disagreement is shown, never hidden. */
export function crossCheck(items: { name: string; generic: string | null }[], drugs: Drug[]) {
  return items.map(it => {
    const m = matchDrug(it.name, null, drugs);
    const offline = m.generic ?? (m.candidates.length === 1 ? m.candidates[0] : null);
    const cloud = it.generic?.toLowerCase().trim() || null;
    const status: CrossStatus = offline && cloud ? (offline === cloud ? 'agree' : 'mismatch') : cloud ? 'cloud_only' : offline ? 'offline_only' : 'unknown';
    return { name: it.name, offline, status };
  });
}

const key = (s: string) => s.toLowerCase().normalize('NFD').replace(/[^a-z0-9]/g, '');
/** Only places Google Maps itself returned (grounding chunks) are shown; the model's own names are never trusted alone. */
export function groundedPlaces<P extends { name: string }>(places: P[], chunks: { title?: string; uri?: string; placeId?: string }[]) {
  const titled = chunks.filter(c => c.title);
  return places.flatMap(p => {
    const k = key(p.name);
    const c = titled.find(x => { const t = key(x.title!); return t === k || t.includes(k) || k.includes(t); });
    return c ? [{ ...p, uri: c.uri ?? null, placeId: c.placeId ?? null }] : [];
  });
}

/** Ask Kit may only answer from today's notices: an answer must cite a real card, otherwise it is discarded. */
export const urgentWordsIn = (text: string, words: string[]) => { const t = ` ${text.toLowerCase()} `; return words.some(w => t.includes(w.toLowerCase())); };
export const KIT_INTENTS = ['greeting', 'where_to_go', 'help', 'pickup', 'family', 'notice', 'unknown'] as const;
export type KitIntent = (typeof KIT_INTENTS)[number];
/** Ask Kit: the model only picks what the person wants; the app answers with its own words and real screens.
 *  Only a notice answer shows model text, and only if it cites a real notice. Urgent words always mean help. */
export function askKitAnswer(result: { intent?: string; cardId: string | null; answer: string }, cardIds: string[], text = '', urgentWords: string[] = []) {
  if (urgentWordsIn(text, urgentWords)) return { intent: 'help' as KitIntent, answer: null, cardId: null };
  const real = !!result.cardId && cardIds.includes(result.cardId) && !!result.answer?.trim();
  const intent: KitIntent = (KIT_INTENTS as readonly string[]).includes(result.intent ?? '') ? (result.intent as KitIntent) : real ? 'notice' : 'unknown';
  if (intent === 'notice') return real ? { intent, answer: result.answer.trim(), cardId: result.cardId } : { intent: 'unknown' as KitIntent, answer: null, cardId: null };
  return { intent, answer: null, cardId: null };
}

const ICON_KIND: Record<string, string> = { medicine: 'medicine', doctor: 'sick', food: 'food', water: 'water', toilet: 'toilet', baby: 'baby', move: 'moving' };
/** "I need help" in the evacuee's own words. Urgency can only go up: the keyword list wins over a calm AI answer.
 *  No device model -> keyword rules and the original words (this task never goes to the cloud). */
export function triageHelp(text: string, ai: { kind: string; urgent: boolean; summary: string } | null, urgentWords: string[], icons: string[]) {
  const t = ` ${text.toLowerCase()} `;
  const urgentWord = urgentWords.some(w => t.includes(w.toLowerCase()));
  if (ai) return { kind: ai.kind, urgent: ai.urgent || urgentWord, summary: ai.summary, by: 'device' as const };
  const kind = icons.map(i => ICON_KIND[i]).find(Boolean) ?? (urgentWord ? 'sick' : 'other');
  return { kind, urgent: urgentWord, summary: text.trim(), by: 'keywords' as const };
}
