// The three "cloud makes it better" features. Each one keeps the person on the device and sends only an anonymous question.
// 1 Medicine check: drug names + strengths only -> generic names, swaps for a pharmacist, interactions (never auto-accepted).
// 2 Find it open: Grounding with Google Maps near a rounded location -> only places Google Maps returned.
// 5 Ask Kit: an evacuee's question answered only from today's shown notices (device first), or "ask a volunteer".
import keywords from '../../../shared/rules/keywords.json';
import { askKitAnswer, coarseLocation, crossCheck, groundedPlaces, medCheckInput, triageHelp } from '../domain/hybrid';
import { keywordIcons } from '../domain/keywordIcons';
import { drugList as drugs, regionPack as pack } from './regionData';
import type { Deps } from './deps';
import type { Job, MedCheck, OpenPlace } from './state';

type CheckOut = { items: MedCheck['items']; interactions: MedCheck['interactions'] };
type OpenOut = { places: Omit<OpenPlace, 'uri' | 'placeId'>[] };

export function createHybrid(d: Deps, enqueue: (type: string, payload: unknown, path?: string | null) => unknown) {
  const setCheck = (personId: string, check: MedCheck) =>
    d.store.set(s => ({ ...s, cards: s.cards.map(c => (c.personId === personId ? { ...c, check } : c)) }));

  async function checkMeds(personId: string, fromQueue = false): Promise<'done' | 'queued' | 'failed'> {
    const card = d.store.get().cards.find(c => c.personId === personId);
    if (!card?.meds.length) return 'failed';
    const input = medCheckInput(card.meds);
    const r = await d.router.run<CheckOut>('med_check', { text: JSON.stringify(input) });
    if (r.status === 'ok' && r.data) {
      setCheck(personId, { ...r.data, cross: crossCheck(r.data.items, drugs.drugs), source: 'cloud', at: d.clock.now(), pending: false });
      return 'done';
    }
    if (r.status === 'queued') {
      setCheck(personId, { items: [], interactions: [], cross: [], source: 'cloud', at: d.clock.now(), pending: true });
      if (!fromQueue) enqueue('med_check', { personId }, null);
      return 'queued';
    }
    return 'failed';
  }

  /** The device decides where it is; only a rounded point (about 1 km) goes to the cloud. */
  function here() {
    const s = d.store.get();
    const sh = pack.shelters.find(x => x.id === s.shelter.id);
    const p = s.volunteerPos ?? (sh ? { lat: sh.lat, lng: sh.lng } : null);
    return p && coarseLocation(p.lat, p.lng, d.rules.findOpen.precisionDecimals);
  }

  async function findOpen(need: string, fromQueue = false): Promise<'done' | 'queued' | 'failed' | 'no_location'> {
    const location = here();
    if (!location) return 'no_location';
    const r = await d.router.run<OpenOut>('find_open', { vars: { need: need.replace(/_/g, ' ') }, location });
    if (r.status === 'ok' && r.data) {
      d.store.set(s => ({ ...s, open: { need, places: groundedPlaces(r.data!.places, r.places ?? []), at: d.clock.now(), pending: false } }));
      return 'done';
    }
    if (r.status === 'queued') {
      d.store.set(s => ({ ...s, open: { need, places: [], at: d.clock.now(), pending: true } }));
      if (!fromQueue) enqueue('find_open', { need }, null);
      return 'queued';
    }
    return 'failed';
  }

  /** The question is never stored or synced. No valid card -> null, and the UI says "ask a volunteer". */
  async function askKit(question: string) {
    const s = d.store.get();
    const cards = s.announcements.filter(a => a.shown).slice(0, 8);
    if (!cards.length || !question.trim()) return { answer: null, card: null, source: null };
    const list = cards.map(a => {
      const c = a.cards?.[0];
      return `id=${a.id} text=${[c?.title, c?.time ?? a.times[0], c?.place ?? a.place].filter(Boolean).join(' · ')} (said: ${a.text})`;
    }).join('\n');
    const r = await d.router.run<{ cardId: string | null; answer: string }>('ask_kit', { text: question.trim(), vars: { cards: list, lang: s.lang === 'id' ? 'Indonesian' : 'English' } });
    const ok = (r.status === 'ok' || r.status === 'draft') && r.data ? askKitAnswer(r.data, cards.map(a => a.id)) : null;
    return { answer: ok?.answer ?? null, card: ok ? cards.find(a => a.id === ok.cardId)! : null, source: ok ? r.source! : null };
  }

  /** "I need help" in the evacuee's own words: understood on this phone only (health words are private).
   *  No device model -> keyword rules. Urgency can only go up. Nothing is sent until the evacuee taps Send. */
  async function triage(text: string) {
    const r = await d.router.run<{ kind: string; urgent: boolean; summary: string }>('help_triage', { text: text.trim() });
    const ai = (r.status === 'ok' || r.status === 'draft') && r.source === 'device' ? r.data! : null;
    return triageHelp(text, ai, d.rules.urgentWords, keywordIcons(text, keywords).icons);
  }

  /** Queued cloud questions run again when the signal returns. */
  async function reRun(j: Job) {
    if (j.type === 'med_check') return (await checkMeds((j.payload as { personId: string }).personId, true)) === 'done';
    if (j.type === 'find_open') return (await findOpen((j.payload as { need: string }).need, true)) === 'done';
    return false;
  }

  return { checkMeds, findOpen, askKit, triage, reRun };
}
