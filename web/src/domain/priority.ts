// Field Check priority (PRODUCT.md 6.11c). Pure, deterministic, explainable. Weights come from Remote Config.
import { cmp } from './cmp';
export type Event = 'flood' | 'earthquake' | 'tsunami' | 'landslide';
export type Tier = 'P1' | 'P2' | 'P3';
export type Hazard = { floodClass?: 0 | 1 | 2 | 3; hand?: number; landslideHigh?: boolean; tsunamiZone?: boolean; unknown?: boolean; pinNearby?: boolean };
export type PriorityInput = {
  age?: number | null; mobility?: 0 | 1 | 2 | 3; cantMiss?: boolean; supplyDays?: number; dailyMeds?: boolean;
  hearing?: boolean; dementia?: boolean; alone?: boolean; hoursSinceContact?: number | null; hazard: Hazard;
  override?: { tier: Tier; reason: string } | null;
};
export type Weights = Record<string, number>;

export function hazardPoints(h: Hazard, ev: Event, w: Weights): number {
  if (h.unknown) return w.unknownLocation; // unknown counts as worst case
  let p = 0;
  if (ev === 'flood') {
    if (h.floodClass != null) p = h.floodClass;
    else if (h.hand != null) p = h.hand < w.handDeep ? 3 : h.hand < w.handMedium ? 2 : h.hand <= w.handShallow ? 1 : 0;
  }
  if (ev === 'tsunami' && h.tsunamiZone) p = w.tsunamiZone;
  if ((ev === 'landslide' || ev === 'earthquake' || ev === 'flood') && h.landslideHigh) p += w.landslideHigh;
  return p + (h.pinNearby ? w.pinNearby : 0);
}

export function priority(p: PriorityInput, ev: Event, w: Weights) {
  const hz = hazardPoints(p.hazard, ev, w), why: string[] = [];
  let s = hz;
  if (hz >= 2) why.push('hazard at location');
  if (p.hazard.unknown) why.push('location unknown');
  const mob = p.mobility ?? 0;
  s += mob; if (mob >= 2) why.push('limited mobility');
  const lowSupply = !!p.cantMiss && (p.supplyDays ?? 0) <= w.lowSupplyDays;
  if (p.cantMiss) { s += w.cantMiss + (lowSupply ? w.cantMissLowSupply : 0); why.push(`can't-miss treatment, ${p.supplyDays ?? '?'} day(s) left`); }
  else if (p.dailyMeds) s += w.dailyMeds;
  if (p.hearing) { s += w.hearing; why.push('hard of hearing'); }
  if (p.dementia) { s += w.dementia; why.push('dementia'); }
  if (p.alone) { s += w.alone; why.push('lives alone'); }
  const age = p.age ?? 0;
  if (age >= 85) s += w.age85; else if (age >= 75) s += w.age75;
  if (p.hoursSinceContact == null) { s += w.neverChecked; why.push('never checked'); }
  else if (p.hoursSinceContact > 24) s += w.contactOver24h;
  const cantWalk = mob === 3;
  const override = (cantWalk && hz >= 2) || lowSupply || (!!p.hazard.unknown && (cantWalk || !!p.cantMiss));
  let tier: Tier = override || s >= w.p1 ? 'P1' : s >= w.p2 ? 'P2' : 'P3';
  if (p.override) { tier = p.override.tier; why.push(`staff override: ${p.override.reason}`); }
  return { score: s, tier, why };
}

export type FieldRow = { id: string; distanceM: number | null; person: PriorityInput };

/** Tier first, then score (high first), then distance from the volunteer (near first, unknown last). */
export function sortFieldList(rows: FieldRow[], ev: Event, w: Weights) {
  return rows
    .map(r => ({ ...r, ...priority(r.person, ev, w) }))
    .sort((a, b) => cmp(a.tier, b.tier) || b.score - a.score || (a.distanceM ?? Infinity) - (b.distanceM ?? Infinity));
}
