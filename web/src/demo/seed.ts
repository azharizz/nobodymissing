// DEMO ONLY: the synthetic Aceh shelter (shared/demo/shelter.json). Loaded in demo mode or from Settings.
import seed from '../../../shared/demo/shelter.json';
import { matchDrug } from '../domain/drugMatcher';
import { drugList } from '../features/regionData';
import type { FieldEntry, MedEntry, State } from '../features/state';

export function demoState(now: number): Partial<State> {
  return {
    shelter: seed.shelter,
    people: seed.people.map(p => ({ ...p, sex: p.sex as 'M' | 'F', status: 'safe' as const, suggestedFlags: p.flags, notes: null, needsReview: false, inferredBy: 'staff' as const, confirmedBy: 'staff' as const })),
    cards: seed.cards.map(c => ({ personId: c.personId, meds: c.meds.map(m => ({ ...m, match: matchDrug(m.nameAsWritten, m.strength, drugList.drugs), confirmedBy: m.confirmedBy as MedEntry['confirmedBy'] })) })),
    field: seed.field.map(f => ({
      ...f, person: f.person as FieldEntry['person'], override: null, status: 'unchecked' as const,
      lastContactAt: f.lastContactAt == null ? null : now - parseFloat(f.lastContactAt) * 3600_000,
    })),
  };
}

/** Demo homes moved onto real low ground (region pack v2). Phones that loaded the demo earlier keep stale spots: fix only demo ids. */
export function refreshDemoLocations(field: FieldEntry[]): FieldEntry[] {
  const at = new Map(seed.field.map(f => [f.id, f.location]));
  return field.map(e => (at.has(e.id) ? { ...e, location: at.get(e.id) as FieldEntry['location'] } : e));
}
