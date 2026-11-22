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
import { createFirstHours } from './firstHours';
import { createQueue } from './queue';
import { createRoster } from './roster';
import { createScout } from './scout';
import { createStore, type Job, type Seed } from './state';
import { demoState } from '../demo/seed';

export type Features = ReturnType<typeof createFeatures>;

export function createFeatures(a: Adapters, rules: Rules = defaults, prompts: Record<string, string> = pack.tasks, seed?: Seed) {
  const store = createStore(a.storage, a.clock.now(), seed);
  const router = createInferenceRouter(a.inference, rules, () => ({ online: store.get().signal === 'back', privateMode: store.get().privateMode }), prompts,
    src => store.set(s => { const day = new Date(a.clock.now()).toDateString(), c = s.counts?.day === day ? s.counts : { day, device: 0, cloud: 0 }; return { ...s, counts: { ...c, [src]: c[src] + 1 } }; }),
    l => { if (l.task !== 'warm') store.set(s => ({ ...s, kitLog: [...(s.kitLog ?? []), { ...l, at: a.clock.now() }].slice(-60) })); });
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
  const familyGroup = createFamilyGroup(d);
  const first = createFirstHours(d, queue.enqueue, roster);
  const scout = createScout(d, async () => { await queue.flush(); await familyGroup.sync(); await first.syncPickups(); }, roster.processBatch);
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
    counts: () => {
      const s = store.get(), c = s.counts?.day === new Date(a.clock.now()).toDateString() ? s.counts : { device: 0, cloud: 0 };
      return { device: c.device, cloud: c.cloud, waiting: s.jobs.filter(j => j.state !== 'sent').length };
    },
    /** Where Kit ran today, and what left the shelter (by kind; device runs send nothing). */
    kit: () => {
      const s = store.get(), day = new Date(a.clock.now()).toDateString();
      const log = (s.kitLog ?? []).filter(l => new Date(l.at).toDateString() === day);
      const sends = (rules as unknown as { cloudSends: Record<string, string> }).cloudSends ?? {};
      const sent = { photo: 0, name: 0, drugs: 0, location: 0, text: 0 } as Record<string, number>;
      for (const l of log) if (l.source === 'cloud' && sends[l.task] in sent) sent[sends[l.task]]++;
      return { log: [...log].reverse(), sent, device: log.filter(l => l.source === 'device').length, cloud: log.filter(l => l.source === 'cloud').length };
    },
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

  return { env: a.env, prepareDevice: a.prepareDevice, store, router, rules, queue, roster, careCard, medicine, announcements, field, scout, family, familyGroup, first, hybrid, views, settings };
}
