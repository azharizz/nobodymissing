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

  it('help triage: understood on the device only; no device = keywords, never the cloud', async () => {
    const { f, inf } = await setup(() => ({ raw: '{"kind":"sick","urgent":false,"summary":"Father has chest pain"}', source: 'device' }));
    f.store.set(s => ({ ...s, signal: 'back' }));
    expect(await f.hybrid.triage('bapak saya nyeri dada')).toMatchObject({ kind: 'sick', urgent: true, by: 'device' });
    expect(inf.calls[0].mode).toBe('only_on_device');
    const off = await setup(undefined, 'unavailable');
    off.f.store.set(s => ({ ...s, signal: 'back' }));
    expect(await off.f.hybrid.triage('need insulin')).toMatchObject({ kind: 'medicine', by: 'keywords' });
    expect(off.inf.calls.length).toBe(0);
  });

  it('family group: QR carries the group offline; online, gossip joins groups that met through different relatives', async () => {
    const cloud = new MemorySync();
    const mk = async () => { const ad = await createAdapters('test'); ad.sync = cloud; return createFeatures(ad, undefined, undefined, demoState); };
    const a = await mk(), b = await mk(), c = await mk();
    a.familyGroup.setMe('Cut Nurlaila', 'safe'); b.familyGroup.setMe('Teuku Hasballah', 'need_help'); c.familyGroup.setMe('M. Rizki', 'safe');
    expect(b.familyGroup.scan(c.familyGroup.myQr()!)!.map(m => m.name)).toEqual(['M. Rizki']);   // b meets c
    expect(a.familyGroup.scan(b.familyGroup.myQr()!)!.map(m => m.name)).toEqual(['Teuku Hasballah', 'M. Rizki']); // a meets b, learns c too, offline
    expect(a.familyGroup.scan('hello')).toBeNull();
    for (const x of [a, b, c]) x.store.set(s => ({ ...s, signal: 'back' }));
    await b.familyGroup.sync(); await a.familyGroup.sync(); await b.familyGroup.sync(); await c.familyGroup.sync(); // gossip rounds (every Scout check when signal is back)
    expect(c.store.get().familyGroup!.members.map(m => m.name).sort()).toEqual(['Cut Nurlaila', 'Teuku Hasballah']); // c never met a
    expect(JSON.stringify([...cloud.docs.values()])).not.toMatch(/flags|needs_meds/);
  });

  it('family search: device understands the description, cloud adds spellings (name only), match is deterministic', async () => {
    const { f, inf } = await setup(r => r.task === 'family_query'
      ? { raw: '{"name":"Nurhayati","age":80,"place":"Sukajadi"}', source: 'device' }
      : { raw: '{"variants":["Nur Hayati"]}', source: 'cloud' });
    f.store.set(s => ({ ...s, signal: 'back', people: [...s.people, { ...s.people[0], id: 'x', nameAsWritten: 'Nur Hayati', age: 82 }] }));
    const r = await f.family.smart('nenek saya Nurhayati, sekitar 80, dari Sukajadi');
    expect(r).toMatchObject({ name: 'Nurhayati', age: 80, parsedBy: 'device', variantsBy: 'cloud' });
    expect(r.hits.map(h => [h.name, h.match])).toEqual([['Nur Hayati', 'spelling']]);
    expect(inf.calls.find(c => c.task === 'family_query')!.mode).toBe('only_on_device');
    expect(inf.calls.find(c => c.task === 'name_variants')!.text).toBe('Nurhayati'); // only the name leaves
  });

  it('first hours: warning on the phone, the higher place offline, pickup waits for signal, staff see it ranked', async () => {
    const { f, inf, sync } = await setup(r => ({ raw: r.task === 'warning_plain' ? '{"hazard":"flood","level":"prepare","plain":"River rising."}' : r.task === 'pickup_request' ? '{"who":"Ibu saya","mobility":1,"needs":[],"people":1}' : '{}', source: 'device' }));
    const w = await f.first.warn('Warga bantaran diminta evakuasi segera, banjir.');
    expect(w).toMatchObject({ hazard: 'flood', level: 'go_now', by: 'device' });           // go-now words beat the model
    expect(inf.calls[0].mode).toBe('prefer_on_device');
    f.first.setPos({ lat: 4.2815, lng: 98.0565 });                                          // real low ground in Kuala Simpang
    expect(f.first.destination()).toMatchObject({ name: "Masjid Jami' al Chairitah", direction: 'N', gps: true });
    const p = await f.first.understandPickup('Ibu saya tidak bisa jalan, perlu insulin');
    expect(p).toMatchObject({ mobility: 3, needs: ['insulin'], by: 'device' });           // mobility only goes up
    expect(inf.calls.find(c => c.task === 'pickup_request')!.mode).toBe('only_on_device');
    const rec = f.first.sendPickup(p);
    expect(sync.docs.size).toBe(0);                                                         // no signal yet
    f.store.set(s => ({ ...s, signal: 'back' })); await f.queue.flush();
    const path = [...sync.docs.keys()].find(k => k.includes('/pickups/'))!;
    expect(path).toMatch(/^shelters\/[^/]+\/pickups\//);
    // Staff phone of that shelter
    const staff = await setup();
    (staff.f.store as any).set((s: any) => ({ ...s, signal: 'back', shelter: { id: path.split('/')[1], name: 'x' } }));
    expect(staff.f.first.importPickup(rec)).toBe(true);
    expect(staff.f.first.importPickup(rec)).toBe(false);                                   // no duplicates
    const row = staff.f.views.field().find(r => r.id === `pk-${rec.id}`)!;
    expect(row.tier).toBe('P1');
  });

  it('first hours: latest official warning uses Google Search grounding (cloud, rounded location)', async () => {
    const { f, inf } = await setup(() => ({ raw: '{"hazard":"flood","level":"go_now","plain":"Leave now.","source":"BPBD"}', source: 'cloud', places: [{ title: 'bpbd.go.id', uri: 'https://bpbd.go.id/x' }] }));
    expect(await f.first.latest()).toBe('offline');
    f.store.set(s => ({ ...s, signal: 'back', myPos: { lat: 4.281234, lng: 98.056789 } }));
    expect(await f.first.latest()).toBe('done');
    expect(inf.calls[0]).toMatchObject({ grounding: 'search', location: { lat: 4.28, lng: 98.06 }, mode: 'only_in_cloud' });
    expect(f.store.get().warning).toMatchObject({ level: 'go_now', by: 'cloud', sources: [{ uri: 'https://bpbd.go.id/x' }] });
  });

  it('check-in at the door: spoken sentence on the device, keyword flags never dropped, joins reach list', async () => {
    const { f, inf } = await setup(() => ({ raw: '{"name":"Hasan Basri","age":82,"flags":["hard_of_hearing"]}', source: 'device' }));
    const c = await f.first.understandCheckin('Hasan Basri, 82, tidak bisa dengar, pakai insulin');
    expect(c).toMatchObject({ name: 'Hasan Basri', age: 82, flags: ['hard_of_hearing', 'needs_meds'], by: 'device' });
    expect(inf.calls[0].mode).toBe('only_on_device');
    const id = f.first.checkin(c);
    expect(f.views.reach().list.find(r => r.id === id)).toBeTruthy();
  });
});
