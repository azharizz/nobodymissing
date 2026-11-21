// F12-lite: hazard-aware Field Check list. Scoring is pure code (domain/priority); AI only structures notes.
import { regionPack as pack } from './regionData';
import { distanceM, handAt, nearestHigher } from '../domain/regionPack';
import { sortFieldList, type Event, type Tier } from '../domain/priority';
import { type Deps, uid } from './deps';
import type { FieldEntry } from './state';

export function fieldList(d: Deps) {
  const s = d.store.get(), now = d.clock.now();
  const shelter = pack.shelters.find(x => x.id === s.shelter.id);
  const from = s.volunteerPos ?? (shelter ? { lat: shelter.lat, lng: shelter.lng } : null);
  const rows = s.field.map(e => {
    const hand = e.location ? handAt(pack, e.location.lat, e.location.lng) : null;
    const hazard = e.location ? { ...e.person.hazard, hand: hand ?? undefined } : { ...e.person.hazard, unknown: true };
    return {
      id: e.id, distanceM: e.location && from ? Math.round(distanceM(from.lat, from.lng, e.location.lat, e.location.lng)) : null,
      person: { ...e.person, hazard, override: e.override, hoursSinceContact: e.lastContactAt == null ? null : (now - e.lastContactAt) / 3600_000 },
    };
  });
  const byId = new Map(s.field.map(e => [e.id, e]));
  return sortFieldList(rows, d.rules.currentEvent as Event, d.rules.fieldPriority).map(r => {
    const e = byId.get(r.id)!;
    const higher = e.location ? nearestHigher(pack, e.location.lat, e.location.lng, d.rules.currentEvent, d.rules.higherMinGainM) : null;
    return { ...r, name: e.name, status: e.status, needsReview: !!e.needsReview, higher };
  });
}

type Report = { people: { name: string; age?: number | null; mobility?: number | null; medicalNeeds?: string[]; waterAsReported?: string | null }[] };

export function createFieldCheck(d: Deps, enqueue: (type: string, payload: unknown) => unknown) {
  const patch = (id: string, p: Partial<FieldEntry>) => d.store.set(s => ({ ...s, field: s.field.map(e => (e.id === id ? { ...e, ...p } : e)) }));
  return {
    override: (id: string, tier: Tier, reason: string) => patch(id, { override: { tier, reason, at: d.clock.now() } }),
    visited: (id: string, status: FieldEntry['status']) => patch(id, { status, lastContactAt: d.clock.now() }),
    async report(text: string, location: FieldEntry['location'], staffApprovedCloud = false) {
      const r = await d.router.run<Report>('field_report', { text }, { staffApprovedCloud });
      if ((r.status === 'ok' || r.status === 'draft') && r.data) {
        const cantMissWords = Object.keys(d.rules.cantMiss);
        const entries: FieldEntry[] = r.data.people.map(p => {
          const needs = (p.medicalNeeds ?? []).map(n => n.toLowerCase());
          const cantMiss = needs.some(n => cantMissWords.some(w => n.includes(w)));
          return {
            id: uid(), name: p.name, location, lastContactAt: d.clock.now(), override: null, status: 'unchecked', needsReview: true,
            person: { age: p.age ?? null, mobility: (p.mobility ?? 0) as 0 | 1 | 2 | 3, cantMiss, supplyDays: cantMiss ? 0 : undefined, dailyMeds: !cantMiss && needs.length > 0 },
          };
        });
        d.store.set(s => ({ ...s, field: [...s.field, ...entries] }));
        enqueue('field_report', { location, people: r.data.people, at: d.clock.now() });
      }
      return r;
    },
  };
}
