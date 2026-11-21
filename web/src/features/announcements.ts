// F2 + F8: speak/type once, everyone sees it. Keyword icons work offline at once; AI cards follow
// (device first, cloud when measured better). Staff approve before anything is shown. Reach list from roster flags.
import keywords from '../../../shared/rules/keywords.json';
import { keywordIcons } from '../domain/keywordIcons';
import { type Deps, uid } from './deps';
import type { Announcement, Job } from './state';

export function createAnnouncements(d: Deps, enqueue: (type: string, payload: unknown, path?: string | null) => unknown) {
  const patch = (id: string, p: Partial<Announcement>) => d.store.set(s => ({ ...s, announcements: s.announcements.map(a => (a.id === id ? { ...a, ...p } : a)) }));
  const get = (id: string) => d.store.get().announcements.find(a => a.id === id)!;

  async function cards(id: string): Promise<'done' | 'queued' | 'failed'> {
    const a = get(id);
    const r = await d.router.run('announcement' + (a.lang === 'id' ? '_id' : ''), { text: a.text });
    if ((r.status === 'ok' || r.status === 'draft') && r.data) { patch(id, { cards: r.data.cards, source: r.source!, draft: r.status === 'draft' }); return 'done'; }
    return r.status === 'queued' ? 'queued' : 'failed';
  }

  async function create(text: string): Promise<Announcement> {
    const lang = d.store.get().lang;
    const a: Announcement = { id: uid(), text, lang, ...keywordIcons(text, keywords), cards: null, source: 'keywords', draft: false, shown: false, told: [], createdAt: d.clock.now() };
    d.store.set(s => ({ ...s, announcements: [a, ...s.announcements] }));
    if ((await cards(a.id)) === 'queued') enqueue('announcement_cards', { annId: a.id }, null);
    return get(a.id);
  }

  function approve(id: string) {
    patch(id, { shown: true });
    const a = get(id);
    enqueue('other', { text: a.text, cards: a.cards, icons: a.icons, times: a.times, place: a.place, createdAt: a.createdAt }, `shelters/${d.store.get().shelter.id}/announcements/${id}`);
  }

  const toggleTold = (id: string, personId: string) =>
    patch(id, { told: get(id).told.includes(personId) ? get(id).told.filter(x => x !== personId) : [...get(id).told, personId] });

  /** Re-run of a queued AI job when the signal returns (called by the queue). */
  const reRun = async (j: Job) => j.type === 'announcement_cards' && (await cards((j.payload as { annId: string }).annId)) === 'done';

  return { create, approve, toggleTold, reRun };
}
