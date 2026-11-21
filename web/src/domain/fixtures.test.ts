// Contract tests: the same shared/fixtures run here (Vitest) and on Android (JUnit).
import { describe, expect, it } from 'vitest';
import rules from '../../../shared/rules/defaults.json';
import keywords from '../../../shared/rules/keywords.json';
import drugs from '../../../shared/drugs/demo-drugs.json';
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
