// F4 care card + F9 Medicine Round. Photo read on the device; drug names matched by deterministic code;
// only confirmed medicines enter the round; requests leave the device as counts by generic name.
import drugs from '../../../shared/drugs/demo-drugs.json';
import { matchDrug } from '../domain/drugMatcher';
import { buildSchedule, combineRequests } from '../domain/schedule';
import type { ImageInput } from '../ports';
import type { Deps } from './deps';
import type { MedEntry } from './state';

type AiMed = { nameAsWritten: string; strength?: string | null; doseAsWritten?: string | null; timesPerDay?: number | null; qtyLeft?: number | null };

export function createCareCard(d: Deps) {
  async function read(personId: string, image: ImageInput, staffApprovedCloud = false) {
    const r = await d.router.run<{ meds: AiMed[]; conditions?: string[]; allergies?: string[] }>('care_card_read', { image }, { staffApprovedCloud });
    if ((r.status === 'ok' || r.status === 'draft') && r.data) {
      const meds: MedEntry[] = r.data.meds.map(m => ({
        nameAsWritten: m.nameAsWritten, strength: m.strength ?? null, doseAsWritten: m.doseAsWritten ?? null, timesPerDay: m.timesPerDay ?? null,
        qtyLeft: m.qtyLeft ?? null, match: matchDrug(m.nameAsWritten, m.strength ?? null, drugs.drugs), generic: null, confirmedBy: null, inferredBy: r.source,
      }));
      d.store.set(s => {
        const old = s.cards.find(c => c.personId === personId);
        const card = { personId, meds: [...(old?.meds ?? []), ...meds], conditions: r.data!.conditions, allergies: r.data!.allergies };
        return { ...s, cards: [...s.cards.filter(c => c.personId !== personId), card] };
      });
    }
    return r;
  }

  /** A human picks the generic from the matcher's result and confirms. Never auto-accepted. */
  function confirmMed(personId: string, index: number, generic: string, by: NonNullable<MedEntry['confirmedBy']>) {
    d.store.set(s => ({ ...s, cards: s.cards.map(c => (c.personId !== personId ? c : { ...c, meds: c.meds.map((m, i) => (i === index ? { ...m, generic, confirmedBy: by } : m)) })) }));
  }

  return { read, confirmMed };
}

export function scheduleOf(d: Deps) {
  const cards = d.store.get().cards.map(c => ({ personId: c.personId, meds: c.meds.filter(m => m.confirmedBy && m.generic).map(m => ({ ...m })) }));
  return buildSchedule(cards, d.rules);
}

export function createMedicine(d: Deps, enqueue: (type: string, payload: unknown) => unknown) {
  const today = () => new Date(d.clock.now()).toISOString().slice(0, 10);
  const key = (slot: string, personId: string, generic: string) => `${today()}|${slot}|${personId}|${generic}`;
  return {
    key,
    mark(slot: string, personId: string, generic: string, status: 'taken' | 'refused' | 'missing') {
      d.store.set(s => ({ ...s, round: { ...s.round, [key(slot, personId, generic)]: status } }));
    },
    requestSupply() {
      const s = scheduleOf(d), sid = d.store.get().shelter.id;
      if (s.alerts.length) {
        const counts: Record<string, number> = {};
        for (const a of s.alerts) counts[a.type] = (counts[a.type] ?? 0) + 1;
        enqueue('cant_miss_alert', { shelterId: sid, counts, minSupplyDays: Math.min(...s.alerts.map(a => a.supplyDays ?? 99)) });
      }
      if (s.shortages.length) {
        const form = (g: string) => drugs.drugs.find(x => x.generic === g)?.form ?? 'unknown';
        enqueue('med_shortage', combineRequests([{ shelterId: sid, items: s.shortages.map(x => ({ generic: x.generic, strength: x.strength ?? '', form: form(x.generic), qty: x.people })) }]));
      }
    },
  };
}
