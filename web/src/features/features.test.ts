import { describe, expect, it } from 'vitest';
import { createAdapters } from '../adapters/adapterFactory';
import { FakeConnectivity, FakeInference, MemorySync } from '../adapters/fakes';
import { createFeatures } from './featureFactory';
import { demoState } from '../demo/seed';

async function setup(reply?: ConstructorParameters<typeof FakeInference>[1], status = 'available') {
  const a = await createAdapters('test');
  a.inference = new FakeInference(status, reply);
  return { a, f: createFeatures(a, undefined, undefined, demoState), inf: a.inference as FakeInference, sync: a.sync as MemorySync, net: a.connectivity as FakeConnectivity };
}
const IMG = { mimeType: 'image/jpeg' as const, base64: 'AAAA' };

describe('features (wired by FeatureFactory with test adapters)', () => {
  it('roster scan: drafts need staff confirmation before they count', async () => {
    const { f } = await setup(() => ({ raw: '{"rows":[{"name":"Hasan Basri","age":82,"flags":["hard_of_hearing"],"confidence":0.6}]}', source: 'device' }));
    const before = f.store.get().people.length;
    expect((await f.roster.scan(IMG)).status).toBe('ok');
    const p = f.store.get().people.at(-1)!;
    expect(p).toMatchObject({ nameAsWritten: 'Hasan Basri', confirmedBy: null, needsReview: true, inferredBy: 'device' });
    expect(f.store.get().people.length).toBe(before + 1);
    expect(f.views.reach().list.find(r => r.id === p.id)).toBeUndefined(); // unconfirmed flags are not used yet
    f.roster.confirm(p.id, { flags: ['hard_of_hearing', 'elderly'] });
    expect(f.views.reach().list.find(r => r.id === p.id)?.reasons).toEqual(['hard_of_hearing', 'age 75+']);
  });

  it('care card: only confirmed medicines enter the Medicine Round', async () => {
    const { f } = await setup(() => ({ raw: '{"meds":[{"nameAsWritten":"Norvasc","strength":"5 mg","doseAsWritten":"1 tab","timesPerDay":1,"qtyLeft":1,"confidence":0.9}]}', source: 'device' }));
    const pid = f.store.get().people.find(p => p.id === 'p-budi')!.id;
    const before = f.views.schedule().counts['08:00'] ?? 0;
    await f.careCard.read(pid, IMG);
    const med = f.store.get().cards.find(c => c.personId === pid)!.meds[0];
    expect(med.match.generic).toBe('amlodipine');
    expect(med.confirmedBy).toBeNull();
    expect(f.views.schedule().counts['08:00'] ?? 0).toBe(before);
    f.careCard.confirmMed(pid, 0, 'amlodipine', 'self');
    expect(f.views.schedule().counts['08:00']).toBe(before + 1);
  });

  it('announcement offline: keyword icons immediately, AI job queued, approve before showing', async () => {
    const { f } = await setup(undefined, 'no_api');
    f.store.set(s => ({ ...s, signal: 'no_signal' }));
    const a = await f.announcements.create('Makan siang jam 12.00 di aula');
    expect(a).toMatchObject({ icons: ['food'], times: ['12:00'], place: 'aula', source: 'keywords', shown: false });
    expect(f.store.get().jobs.some(j => j.type === 'announcement_cards' && j.state === 'waiting')).toBe(true);
    f.announcements.approve(a.id);
    expect(f.views.news()[0].id).toBe(a.id);
  });

  it('scout: signal back after 2 real checks -> alert, then queue syncs in priority order', async () => {
    const { f, sync, net } = await setup();
    f.store.set(s => ({ ...s, jobs: [] }));
    f.queue.enqueue('roster_merge', { n: 1 });
    f.medicine.requestSupply();
    net.online = false;
    await f.scout.check();
    expect(f.store.get().signal).toBe('no_signal');
    net.online = true;
    await f.scout.check();
    expect(f.store.get().scoutAlert).toBe(false);
    await f.scout.check();
    expect(f.store.get().scoutAlert).toBe(true);
    const types = [...sync.docs.keys()].map(k => k.split('/')[0]);
    expect(types[0]).toBe('alerts');
    expect(types).toContain('requests');
    expect(f.store.get().jobs.every(j => j.state === 'sent')).toBe(true);
  });

  it('supply request leaves the device with counts only, no names', async () => {
    const { f, sync } = await setup();
    f.store.set(s => ({ ...s, jobs: [] }));
    f.medicine.requestSupply();
    await f.queue.flush();
    const docs = JSON.stringify([...sync.docs.values()]);
    expect(docs).not.toMatch(/Hasan|Sari|p-hasan/);
    expect(docs).toMatch(/insulin glargine/);
  });

  it('courier: worker exports its queue, scout imports it', async () => {
    const w = await setup(); const s = await setup();
    w.f.store.set(st => ({ ...st, jobs: [] })); s.f.store.set(st => ({ ...st, jobs: [] }));
    w.f.queue.enqueue('family_update', { name: 'x' });
    const chunks = w.f.queue.exportChunks(50);
    expect(s.f.queue.importChunks(chunks)).toBe(1);
    expect(s.f.store.get().jobs[0].type).toBe('family_update');
  });

  it('field check: list sorted P1 first with reasons, override logged', async () => {
    const { f } = await setup();
    const list = f.views.field();
    expect(list[0].tier).toBe('P1');
    expect(list[0].why.length).toBeGreaterThan(0);
    f.field.override(list[0].id, 'P3', 'family confirmed safe');
    expect(f.views.field().find(r => r.id === list[0].id)!.tier).toBe('P3');
  });

  it('family search: local shelter only when offline, minimal fields', async () => {
    const { f } = await setup();
    f.store.set(s => ({ ...s, signal: 'no_signal' }));
    const r = await f.family.search('Hasan');
    expect(r.cloud).toBe(false);
    expect(r.hits[0]).toEqual({ name: 'Pak Hasan Basri', ageBand: '80-89', shelterId: 'SD3', status: 'safe' });
  });

  it('batch mode: an approved photo with no signal waits in the batch and is read when processed', async () => {
    const { f } = await setup(() => ({ raw: '{"rows":[{"name":"Cut Nur","age":30,"confidence":0.9}]}', source: 'cloud' }), 'no_api');
    f.store.set(s => ({ ...s, signal: 'no_signal' }));
    expect((await f.roster.scan(IMG, true)).status).toBe('queued');
    expect(f.store.get().batch).toHaveLength(1);
    f.store.set(s => ({ ...s, signal: 'back' }));
    await f.roster.processBatch();
    expect(f.store.get().batch).toHaveLength(0);
    expect(f.store.get().people.some(p => p.nameAsWritten === 'Cut Nur')).toBe(true);
  });

  it('AI card times are normalised by the deterministic parser ("8 am" -> 08:00)', async () => {
    const { f } = await setup(() => ({ raw: '{"cards":[{"icon":"medicine","title":"Medicine Round","time":"8 am","place":"Health Post"}]}', source: 'device' }));
    f.store.set(s => ({ ...s, signal: 'back' }));
    const a = await f.announcements.create('Medicine round at 8 am at the health post');
    expect(a.cards![0].time).toBe('08:00');
    expect(a.source).toBe('device');
  });

  it('a real (non-demo) shelter starts empty; the synthetic demo loads only on request', async () => {
    const a = await createAdapters('test');
    const f = createFeatures(a);
    expect(f.store.get().people).toEqual([]);
    expect(f.store.get().field).toEqual([]);
    f.settings.loadDemo();
    expect(f.store.get().people.length).toBeGreaterThan(0);
    expect(f.store.get().shelter.id).toBe('SD3');
  });

  it('medicine check: only drug names leave; offline it waits, then runs when signal returns', async () => {
    const reply = (r: any) => ({ raw: JSON.stringify({ items: [{ name: 'insulin glargine', generic: 'insulin glargine', swaps: [], cantMiss: true }], interactions: [] }), source: 'cloud' as const });
    const { f, inf } = await setup(reply);
    const card = f.store.get().cards[0];
    const who = f.store.get().people.find(p => p.id === card.personId)!;
    expect(await f.hybrid.checkMeds(card.personId)).toBe('queued');      // no signal yet
    expect(inf.calls.length).toBe(0);
    f.store.set(s => ({ ...s, signal: 'back' }));
    await f.queue.flush();
    const req = inf.calls.find(c => c.task === 'med_check')!;
    expect(req.mode).toBe('only_in_cloud');
    expect(req.image).toBeUndefined();
    expect(req.text).not.toContain(who.nameAsWritten);
    const check = f.store.get().cards.find(c => c.personId === card.personId)!.check!;
    expect(check.pending).toBe(false);
    expect(check.cross[0].status).toBe('agree');
    expect(card.meds.every(m => f.store.get().cards[0].meds.find(x => x.nameAsWritten === m.nameAsWritten)!.confirmedBy === m.confirmedBy)).toBe(true); // never auto-accepted
  });

  it('find it open: Maps grounding with a rounded location; invented places are dropped', async () => {
    const { f, inf } = await setup(() => ({ raw: '{"places":[{"name":"Apotek Real","openNow":true},{"name":"Made Up Clinic"}]}', source: 'cloud', places: [{ title: 'Apotek Real', uri: 'https://maps.google.com/?cid=9' }] }));
    f.store.set(s => ({ ...s, signal: 'back', volunteerPos: { lat: 4.234567, lng: 98.004321 } }));
    expect(await f.hybrid.findOpen('pharmacy')).toBe('done');
    const req = inf.calls[0];
    expect(req.grounding).toBe('maps');
    expect(req.location).toEqual({ lat: 4.23, lng: 98 });
    expect(f.store.get().open!.places.map(p => p.name)).toEqual(['Apotek Real']);
  });

  it('Ask Kit: answers only with a real notice, on the device first', async () => {
    const { f, inf } = await setup(r => ({ raw: JSON.stringify({ cardId: /id=(\S+)/.exec(r.prompt)![1], answer: 'Lunch is at 12:00.' }), source: 'device' }));
    f.store.set(s => ({ ...s, announcements: [{ id: 'a1', text: 'Lunch 12.00 in the hall', lang: 'en', icons: ['food'], times: ['12:00'], place: 'hall', cards: null, source: 'keywords', draft: false, shown: true, told: [], createdAt: 0 }] }));
    const r = await f.hybrid.askKit('When is lunch?');
    expect(r).toMatchObject({ answer: 'Lunch is at 12:00.', source: 'device' });
    expect(inf.calls[0].mode).toBe('prefer_on_device');
    const bad = await setup(() => ({ raw: '{"cardId":"nope","answer":"The bus leaves at 9."}', source: 'device' }));
    bad.f.store.set(s => ({ ...s, announcements: f.store.get().announcements }));
    expect((await bad.f.hybrid.askKit('bus?')).answer).toBeNull();
  });
});
