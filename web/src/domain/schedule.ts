// Medicine Round: slots, can't-miss alerts, shortages (6.9). Dose is only ever "as written".
import { cmp } from './cmp';
export type CardMed = { generic: string | null; strength: string | null; doseAsWritten: string | null; timesPerDay: number | null; qtyLeft: number | null };
export type Card = { personId: string; meds: CardMed[] };
type Rules = { slotTimes: Record<string, string[]>; cantMiss: Record<string, string>; shortageDays: number };
export type SlotEntry = { personId: string; generic: string; doseAsWritten: string | null };

export function buildSchedule(cards: Card[], r: Rules) {
  const slots: Record<string, SlotEntry[]> = {};
  const alerts: { personId: string; generic: string; type: string; supplyDays: number | null }[] = [];
  const short = new Map<string, { generic: string; strength: string | null; people: number }>();
  let unscheduled = 0;
  for (const c of cards) for (const m of c.meds) {
    const times = m.timesPerDay ? r.slotTimes[String(m.timesPerDay)] : undefined;
    if (!m.generic || !times) { unscheduled++; continue; }
    for (const t of times) (slots[t] ??= []).push({ personId: c.personId, generic: m.generic, doseAsWritten: m.doseAsWritten });
    const days = m.qtyLeft == null ? null : Math.floor(m.qtyLeft / m.timesPerDay!);
    const type = r.cantMiss[m.generic];
    if (type) alerts.push({ personId: c.personId, generic: m.generic, type, supplyDays: days });
    if (days != null && days <= r.shortageDays) {
      const k = `${m.generic}|${m.strength ?? ''}`;
      const s = short.get(k) ?? { generic: m.generic, strength: m.strength, people: 0 };
      s.people++; short.set(k, s);
    }
  }
  const counts = Object.fromEntries(Object.keys(slots).sort().map(t => [t, slots[t].length]));
  alerts.sort((a, b) => (a.supplyDays ?? Infinity) - (b.supplyDays ?? Infinity) || cmp(a.personId, b.personId));
  const shortages = [...short.values()].sort((a, b) => cmp(a.generic, b.generic) || cmp((a.strength ?? ''), b.strength ?? ''));
  return { slots, counts, alerts, shortages, unscheduled };
}

export type RequestItem = { generic: string; strength: string; form: string; qty: number };
/** Cross-shelter supply request: by generic name, counts only, no evacuee names. */
export function combineRequests(shelters: { shelterId: string; items: RequestItem[] }[]) {
  const m = new Map<string, RequestItem>();
  for (const s of shelters) for (const i of s.items) {
    const k = `${i.generic}|${i.strength}|${i.form}`;
    const cur = m.get(k);
    m.set(k, cur ? { ...cur, qty: cur.qty + i.qty } : { ...i });
  }
  const key = (i: RequestItem) => `${i.generic}|${i.strength}|${i.form}`;
  return {
    fromShelters: [...new Set(shelters.map(s => s.shelterId))].sort(),
    items: [...m.values()].sort((a, b) => cmp(key(a), key(b))),
  };
}
