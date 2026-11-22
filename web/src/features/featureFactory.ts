// FeatureFactory: wires every feature to its ports. Features never construct adapters.
import defaults from '../../../shared/rules/defaults.json';
import pack from '../../../shared/prompts/pack.json';
import { reachList } from '../domain/reachList';
import type { Adapters } from '../ports';
import type { Rules } from '../adapters/firebaseApp';
import { createAnnouncements } from './announcements';
import { createCareCard, createMedicine, scheduleOf } from './careCard';
import type { Deps } from './deps';
import { createFamilySearch } from './familySearch';
import { createFamilyGroup } from './familyGroup';
import { createFieldCheck, fieldList } from './fieldCheck';
import { createInferenceRouter } from './inferenceRouter';
import { createHybrid } from './hybrid';
import { createQueue } from './queue';
import { createRoster } from './roster';
import { createScout } from './scout';
import { createStore, type Job, type Seed } from './state';
import { demoState } from '../demo/seed';

export type Features = ReturnType<typeof createFeatures>;

export function createFeatures(a: Adapters, rules: Rules = defaults, prompts: Record<string, string> = pack.tasks, seed?: Seed) {
  const store = createStore(a.storage, a.clock.now(), seed);
  const router = createInferenceRouter(a.inference, rules, () => ({ online: store.get().signal === 'back', privateMode: store.get().privateMode }), prompts);
  const d: Deps = { store, router, rules, sync: a.sync, connectivity: a.connectivity, clock: a.clock };

  let announcements: ReturnType<typeof createAnnouncements>;
  let hybrid: ReturnType<typeof createHybrid>;
  const queue = createQueue(d, async (j: Job) => (await announcements.reRun(j)) || (await hybrid.reRun(j)));
  hybrid = createHybrid(d, queue.enqueue);
  announcements = createAnnouncements(d, queue.enqueue);
  const roster = createRoster(d, queue.enqueue);
  const careCard = createCareCard(d);
  const medicine = createMedicine(d, queue.enqueue);
  const field = createFieldCheck(d, queue.enqueue);
  const familyGroup = createFamilyGroup(d, queue.enqueue);
  const scout = createScout(d, async () => { await queue.flush(); await familyGroup.sync(); }, roster.processBatch);
  const family = createFamilySearch(d);

  const views = {
    schedule: () => scheduleOf(d),
    field: () => fieldList(d),
    news: () => store.get().announcements.filter(x => x.shown),
    reach: (annId?: string) => {
      const s = store.get();
      const ann = s.announcements.find(x => x.id === annId) ?? s.announcements[0];
      return reachList(s.people.filter(p => p.confirmedBy).map(p => ({ id: p.id, age: p.age, flags: p.flags })), ann?.told ?? [], rules);
    },
    counts: () => router.counts(),
  };

  const settings = {
    setPrivate: (on: boolean) => store.set(s => ({ ...s, privateMode: on })),
    setLang: (lang: 'en' | 'id') => store.set(s => ({ ...s, lang })),
    help: (kind: string, spot: string, text?: string, more: { summary?: string; urgent?: boolean; by?: 'device' | 'keywords' } = {}) =>
      store.set(s => ({ ...s, help: [...s.help, { id: crypto.randomUUID(), kind, spot, text, ...more, at: a.clock.now(), done: false }] })),
    /** DEMO: replace this device's shelter with the synthetic Aceh shelter. */
    loadDemo: () => store.set(s => ({ ...s, ...demoState(a.clock.now()), jobs: [], announcements: [], help: [], round: {} })),
    helpDone: (id: string) => store.set(s => ({ ...s, help: s.help.map(h => (h.id === id ? { ...h, done: true } : h)) })),
  };

  return { env: a.env, prepareDevice: a.prepareDevice, store, router, rules, queue, roster, careCard, medicine, announcements, field, scout, family, familyGroup, hybrid, views, settings };
}
