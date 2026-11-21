// Eval set B scoring (PRODUCT.md 7): a card set is correct when the first card's icon is allowed,
// the time matches exactly (after the deterministic time parser) and the place contains the expected word.
import keywords from '../../../shared/rules/keywords.json';
import { keywordIcons } from '../domain/keywordIcons';

export type Truth = { id: string; lang: string; text: string; icons: string[]; time: string | null; place: string | null };
export type Card = { icon: string; title: string; time?: string | null; place?: string | null };

export function scoreCards(t: Truth, cards: Card[] | null) {
  const c = cards?.[0];
  if (!c) return { icon: false, time: false, place: false, all: false };
  const time = c.time ? keywordIcons(c.time, keywords).times[0] ?? null : null;
  const anyTime = cards!.map(x => (x.time ? keywordIcons(x.time, keywords).times[0] : null)).find(Boolean) ?? null;
  const r = {
    icon: t.icons.includes(c.icon),
    time: (time ?? anyTime) === t.time,
    place: t.place == null ? true : cards!.some(x => (x.place ?? '').toLowerCase().includes(t.place!)),
  };
  return { ...r, all: r.icon && r.time && r.place };
}
