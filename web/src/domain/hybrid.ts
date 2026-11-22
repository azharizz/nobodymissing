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
export function askKitAnswer(result: { cardId: string | null; answer: string }, cardIds: string[]) {
  return result.cardId && cardIds.includes(result.cardId) && result.answer.trim() ? { answer: result.answer.trim(), cardId: result.cardId } : null;
}
