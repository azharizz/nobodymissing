// The first hours (PRODUCT focus): warning -> the way -> pickup -> check-in at the door.
// Phone: plain words, the higher place, pickup and check-in understanding all work with no signal (private words stay here).
// Cloud: the latest official warning (Grounding with Google Search) and delivering pickups + the register when any signal appears.
import { nearestHigher } from '../domain/regionPack';
import { distanceM } from '../domain/regionPack';
import { coarseLocation } from '../domain/hybrid';
import { mergeCheckin, mergePickup, pickupPriority, plainWarning, type Checkin, type Pickup, type PickupRecord } from '../domain/firstHours';
import { maybeAlreadyHere, offlineVariants } from '../domain/family';
import { regionPack as pack } from './regionData';
import { type Deps, online, uid } from './deps';
import type { FieldEntry } from './state';

const EVENTS = ['flood', 'tsunami', 'landslide', 'earthquake'];

export function createFirstHours(d: Deps, enqueue: (type: string, payload: unknown, path?: string | null) => unknown, roster: { addPerson: (n: string, a: number | null) => string; confirm: (id: string, p: { flags?: string[] }) => void }) {
  const lang = () => (d.store.get().lang === 'id' ? 'Indonesian' : 'English');

  /** Any warning text (SMS, WhatsApp, loudspeaker words) -> plain words, on the phone first. */
  async function warn(text: string) {
    const r = await d.router.run<{ hazard: string; level: 'advice' | 'prepare' | 'go_now'; plain: string; action?: string | null }>('warning_plain', { text: text.trim(), vars: { lang: lang() } });
    const ai = (r.status === 'ok' || r.status === 'draft') && r.data ? r.data : null;
    const w = { ...plainWarning(text, ai, r.source ?? 'device', d.rules), text: text.trim(), at: d.clock.now(), sources: [] as { title?: string; uri?: string }[] };
    d.store.set(s => ({ ...s, warning: w }));
    return w;
  }

  /** Online only: the latest official BMKG/BPBD warning for this area (Grounding with Google Search, rounded location). */
  async function latest() {
    const p = here();
    if (!online(d) || !p) return 'offline' as const;
    const r = await d.router.run<{ hazard: string; level: 'advice' | 'prepare' | 'go_now'; plain: string; action?: string | null; source?: string | null }>('warning_latest',
      { vars: { place: pack.name, lang: lang() }, location: coarseLocation(p.lat, p.lng, d.rules.findOpen.precisionDecimals) });
    if (r.status !== 'ok' || !r.data) return 'failed' as const;
    const prev = d.store.get().warning;
    const w = plainWarning(prev?.text ?? '', r.data, 'cloud', d.rules);
    // A newer official answer never lowers a go-now the person already received.
    const level = prev?.level === 'go_now' && w.level !== 'go_now' ? 'go_now' : w.level;
    d.store.set(s => ({ ...s, warning: { ...w, level, text: r.data!.source ?? '', at: d.clock.now(), sources: (r.places ?? []).filter(x => x.uri).slice(0, 3) } }));
    return 'done' as const;
  }

  /** Where am I: GPS if we have it, else the shelter. */
  function here() {
    const s = d.store.get(), sh = pack.shelters.find(x => x.id === s.shelter.id) ?? pack.shelters[0];
    return s.myPos ?? (sh ? { lat: sh.lat, lng: sh.lng } : null);
  }
  /** The higher place for this hazard from the offline flood data. Never "safe", only "higher". */
  function destination() {
    const p = here(), w = d.store.get().warning;
    if (!p) return null;
    const ev = w && EVENTS.includes(w.hazard) ? w.hazard : d.rules.currentEvent;
    const h = nearestHigher(pack, p.lat, p.lng, ev, d.rules.higherMinGainM);
    const sh = h && pack.shelters.find(x => x.id === h.id);
    return h && sh ? { ...h, lat: sh.lat, lng: sh.lng, from: p, gps: !!d.store.get().myPos } : null;
  }
  /** Same position = no update: each update re-renders Show the way, which re-attaches the GPS watch, which reports again. */
  const setPos = (p: { lat: number; lng: number } | null) => {
    const c = d.store.get().myPos;
    if (c === p || (c && p && c.lat === p.lat && c.lng === p.lng)) return;
    d.store.set(s => ({ ...s, myPos: p }));
  };

  /** "My mother can't walk, needs insulin": understood on this phone only; keywords if no model. Not sent yet. */
  async function understandPickup(text: string) {
    const r = await d.router.run<Pickup>('pickup_request', { text: text.trim() });
    const ai = (r.status === 'ok' || r.status === 'draft') && r.source === 'device' && r.data ? r.data : null;
    return mergePickup(ai, text, d.rules.needWords, d.rules.mobilityWords, d.rules.peopleWords);
  }
  /** Send only on the person's tap. Goes to the nearest shelter's pickup list through the outbox (waits for signal). */
  function sendPickup(p: Pickup) {
    const pos = here()!;
    const near = [...pack.shelters].sort((a, b) => distanceM(pos.lat, pos.lng, a.lat, a.lng) - distanceM(pos.lat, pos.lng, b.lat, b.lng))[0];
    const rec: PickupRecord = { ...p, id: uid().slice(0, 12), lat: +pos.lat.toFixed(5), lng: +pos.lng.toFixed(5), at: d.clock.now() };
    const job = enqueue('pickup', rec, `shelters/${near.id}/pickups/${rec.id}`) as { id: string };
    d.store.set(s => ({ ...s, myPickup: { ...rec, jobId: job.id } }));
    return rec;
  }

  /** Staff: a pickup (from sync or a scanned QR) becomes a Field Check entry ranked by the same rules. */
  function importPickup(p: PickupRecord) {
    const id = `pk-${p.id}`;
    if (d.store.get().field.some(e => e.id === id)) return false;
    const e: FieldEntry = { id, name: p.who, location: { lat: p.lat, lng: p.lng }, lastContactAt: p.at, override: null, status: 'unchecked', needsReview: false,
      person: { age: null, ...pickupPriority(p) }, pickup: { needs: p.needs, people: p.people, at: p.at } };
    d.store.set(s => ({ ...s, field: [e, ...s.field] }));
    return true;
  }
  async function syncPickups() {
    if (!online(d)) return;
    try {
      for (const r of await d.sync.list(`shelters/${d.store.get().shelter.id}/pickups`))
        if (typeof r.who === 'string') importPickup({ id: String(r.id), who: r.who, lat: Number(r.lat), lng: Number(r.lng), mobility: Number(r.mobility) || 0, needs: (r.needs as string[]) ?? [], people: Number(r.people) || 1, at: Number(r.at) || 0 });
    } catch { /* offline again */ }
  }

  /** Staff at the door: one spoken sentence -> name, age, flags (on this phone only). */
  async function understandCheckin(text: string) {
    const r = await d.router.run<Checkin>('checkin_parse', { text: text.trim() });
    const ai = (r.status === 'ok' || r.status === 'draft') && r.source === 'device' && r.data ? r.data : null;
    return mergeCheckin(ai, text, d.rules.flagWords);
  }
  /** Confirmed by a person: joins the roster with its flags (tell-in-person and medicine follow from the flags). */
  function checkin(c: Checkin) {
    const id = roster.addPerson(c.name, c.age);
    roster.confirm(id, { flags: c.flags });
    return id;
  }

  /** Before adding: people already here who might be the same person (spelling-aware, age ±10). Staff decide. */
  function alreadyHere(name: string, age: number | null) {
    const s = d.store.get(), people = s.people.filter(p => p.confirmedBy);
    if (!name.trim()) return [];
    const ids = maybeAlreadyHere(name, age, people.map(p => ({ id: p.id, name: p.nameAsWritten, age: p.age, shelterId: s.shelter.id, status: p.status })), offlineVariants(name, d.rules.nameRules));
    return people.filter(p => ids.includes(p.id));
  }

  return { warn, latest, here, destination, setPos, understandPickup, sendPickup, importPickup, syncPickups, understandCheckin, checkin, alreadyHere };
}
