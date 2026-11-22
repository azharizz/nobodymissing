// Contract tests: the same shared/fixtures run here (Vitest) and on Android (JUnit).
import { describe, expect, it } from 'vitest';
import rules from '../../../shared/rules/defaults.json';
import keywords from '../../../shared/rules/keywords.json';
import drugs from '../../../shared/demo/drugs.json';
import priorityFx from '../../../shared/fixtures/priority.json';
import drugFx from '../../../shared/fixtures/drugMatcher.json';
import scheduleFx from '../../../shared/fixtures/schedule.json';
import dedupeFx from '../../../shared/fixtures/dedupe.json';
import queueFx from '../../../shared/fixtures/queue.json';
import routerFx from '../../../shared/fixtures/router.json';
import iconsFx from '../../../shared/fixtures/keywordIcons.json';
import reachFx from '../../../shared/fixtures/reachList.json';
import schemaFx from '../../../shared/fixtures/schemaValidation.json';
import mappingFx from '../../../shared/fixtures/rosterMapping.json';
import scoutFx from '../../../shared/fixtures/scout.json';
import courierFx from '../../../shared/fixtures/courier.json';
import { priority, sortFieldList, type PriorityInput } from './priority';
import { matchDrug, substitutionLevel } from './drugMatcher';
import { buildSchedule, combineRequests } from './schedule';
import { normalizeName, groupDuplicates, familySearch } from './dedupe';
import { applyEvents, orderJobs, progress } from './queue';
import { decide, isOnDeviceReady, type RouterCtx } from './router';
import { keywordIcons } from './keywordIcons';
import { reachList } from './reachList';
import { parseAndValidate, SCHEMAS } from './validate';
import { mapRosterRows } from './rosterMapping';
import { scoutStates } from './scout';
import { chunk, join } from './courier';
import { handAt, nearestHigher } from './regionPack';
import pack from '../../../shared/demo/region-aceh.json';
import regionFx from '../../../shared/fixtures/regionPack.json';

describe('priority', () => {
  for (const c of priorityFx.cases) it(c.name, () => {
    expect(priority(c.input.person as PriorityInput, c.input.event as any, rules.fieldPriority)).toEqual(c.expected);
  });
  for (const c of priorityFx.sortCases) it(c.name, () => {
    expect(sortFieldList(c.input.people as any, c.input.event as any, rules.fieldPriority).map(r => r.id)).toEqual(c.expected);
  });
});

describe('drugMatcher', () => {
  for (const c of drugFx.cases) it(c.name, () => expect(matchDrug(c.input.text, c.input.strength, drugs.drugs)).toEqual(c.expected));
  for (const c of drugFx.substitutionCases) it(c.name, () => expect(substitutionLevel(c.input.prescribed, c.input.available)).toBe(c.expected));
});

describe('schedule', () => {
  for (const c of scheduleFx.cases) it(c.name, () => {
    const s = buildSchedule(c.input.cards as any, rules);
    expect(s.counts).toEqual(c.expected.counts);
    expect(s.slots['12:00']).toEqual(c.expected.slot12);
    expect(s.alerts).toEqual(c.expected.alerts);
    expect(s.shortages).toEqual(c.expected.shortages);
    expect(s.unscheduled).toBe(c.expected.unscheduled);
  });
  for (const c of scheduleFx.requestCases) it(c.name, () => expect(combineRequests(c.input.shelters)).toEqual(c.expected));
});

describe('dedupe', () => {
  for (const c of dedupeFx.normalizeCases) it(`normalize ${c.input}`, () => expect(normalizeName(c.input)).toBe(c.expected));
  for (const c of dedupeFx.groupCases) it(c.name, () => expect(groupDuplicates(c.input.records)).toEqual(c.expected));
  for (const c of dedupeFx.searchCases) it(c.name, () => expect(familySearch(c.input.query, c.input.records as any)).toEqual(c.expected));
});

describe('queue', () => {
  for (const c of queueFx.transitionCases) it(c.name, () => {
    if ('error' in c.expected) expect(() => applyEvents(c.input.events as any)).toThrow();
    else expect(applyEvents(c.input.events as any)).toEqual(c.expected);
  });
  for (const c of queueFx.orderCases) it(c.name, () => expect(orderJobs(c.input.jobs, rules.queuePriority).map(j => j.id)).toEqual(c.expected));
  for (const c of queueFx.progressCases) it('progress', () => expect(progress(c.input.states as any)).toEqual(c.expected));
});

describe('router', () => {
  for (const c of routerFx.cases) it(c.name, () => expect(decide(c.input.task, c.input.ctx as RouterCtx, rules.routerPolicy as any, rules.accuracyBarPoints)).toEqual(c.expected));
  for (const c of routerFx.guardCases) it(`guard: ${c.name}`, () => expect(isOnDeviceReady(c.input.apiPresent, c.input.availability)).toBe(c.expected));
});

describe('keywordIcons', () => {
  for (const c of iconsFx.cases) it(c.input, () => expect(keywordIcons(c.input, keywords)).toEqual(c.expected));
});

describe('reachList', () => {
  for (const c of reachFx.cases) it('reach list', () => expect(reachList(c.input.people, c.input.told, rules)).toEqual(c.expected));
});

describe('schema validation', () => {
  for (const c of schemaFx.cases) it(c.name, () => expect(parseAndValidate(c.input.raw, SCHEMAS[c.input.schema as keyof typeof SCHEMAS]).valid).toBe(c.expected.valid));
});

describe('roster mapping', () => {
  for (const c of mappingFx.cases) it('maps rows', () => expect(mapRosterRows(c.input.rows as any, c.input.source as any, rules)).toEqual(c.expected));
});

describe('scout', () => {
  for (const c of scoutFx.cases) it('signal states', () => expect(scoutStates(c.input.checks, rules.scout.successesNeeded)).toEqual(c.expected));
});

describe('courier', () => {
  for (const c of courierFx.cases) it(`chunk ${c.input.payload}`, () => expect(chunk(c.input.payload, c.input.size)).toEqual(c.expected));
  for (const c of courierFx.joinCases) it(`join ${c.input.join(',')}`, () => expect(join(c.input)).toBe(c.expected));
});

describe('regionPack', () => {
  for (const c of regionFx.handCases) it(`HAND at ${c.input.lat},${c.input.lng}`, () => expect(handAt(pack, c.input.lat, c.input.lng)).toBe(c.expected));
  for (const c of regionFx.higherCases) it('nearest higher shelter for this hazard', () => expect(nearestHigher(pack, c.input.lat, c.input.lng, c.input.event, rules.higherMinGainM)).toEqual(c.expected));
});

import hybridFx from '../../../shared/fixtures/hybrid.json';
import { askKitAnswer, coarseLocation, crossCheck, groundedPlaces, medCheckInput } from './hybrid';
describe('hybrid AI safety contracts (shared fixtures)', () => {
  it('medicine check input is a whitelist: no names, beds or photos', () => {
    for (const c of hybridFx.medCheckInput) {
      const out = medCheckInput(c.input.meds as any);
      expect(out, c.name).toEqual(c.expected);
      expect(JSON.stringify(out)).not.toMatch(/Hasan|Bed 4|base64/);
    }
  });
  it('location is rounded before it leaves', () => { for (const c of hybridFx.coarseLocation) expect(coarseLocation(c.input.lat, c.input.lng, c.input.decimals)).toEqual(c.expected); });
  it('cloud generic names are cross-checked against the offline list', () => { for (const c of hybridFx.crossCheck) expect(crossCheck(c.input.items, drugs.drugs), c.name).toEqual(c.expected); });
  it('only Google Maps places survive', () => { for (const c of hybridFx.groundedPlaces) expect(groundedPlaces(c.input.places as any, c.input.chunks).map(p => ({ name: p.name, uri: p.uri })), c.name).toEqual(c.expected); });
  it('Ask Kit answers must cite a real card', () => { for (const c of hybridFx.askKit) expect(askKitAnswer(c.input.result, c.input.cardIds), c.name).toEqual(c.expected); });
});

import { triageHelp } from './hybrid';
import { decodeFamilyQr, encodeFamilyQr, mergeMembers } from './family';
describe('help triage + family QR (shared fixtures)', () => {
  it('urgency never goes down; no model = keyword rules', () => {
    for (const c of hybridFx.helpTriage) expect(triageHelp(c.input.text, c.input.ai, rules.urgentWords, keywordIcons(c.input.text, keywords).icons), c.name).toEqual(c.expected);
  });
  it('family QR round trip and rejects', () => {
    for (const c of hybridFx.familyQr) { const q = encodeFamilyQr(c.input.fam, c.input.me as any); expect(q).toBe(c.expected); expect(decodeFamilyQr(q)).toEqual({ fam: c.input.fam, member: c.input.me }); }
    for (const bad of hybridFx.familyQrReject) expect(decodeFamilyQr(bad), bad).toBeNull();
  });
  it('family merge: newer wins, nobody dropped', () => { for (const c of hybridFx.familyMerge) expect(mergeMembers(c.input.mine as any, c.input.theirs as any), c.name).toEqual(c.expected); });
});
