// Eval set B runner, in the browser (Chrome with Nano): device vs cloud vs keywords, through Firebase AI Logic.
// Usage (dev server, console): const m = await import('/src/eval/runB.ts'); await m.runB()
import { getApp } from 'firebase/app';
import evalB from '../../../shared/fixtures/evalB-announcements.json';
import keywords from '../../../shared/rules/keywords.json';
import pack from '../../../shared/prompts/pack.json';
import { FirebaseAiLogicInference } from '../adapters/firebaseAiLogic';
import { keywordIcons } from '../domain/keywordIcons';
import { parseAndValidate, SCHEMAS } from '../domain/validate';
import type { Mode } from '../domain/router';
import { scoreCards, type Card, type Truth } from './score';

type Path = { name: string; mode: Mode | null; model: string };
const PATHS: Path[] = [
  { name: 'keywords', mode: null, model: '' },
  { name: 'device', mode: 'only_on_device', model: 'gemini-3.5-flash-lite' }, // model name unused on device, required by the SDK
  { name: 'cloud-lite', mode: 'only_in_cloud', model: 'gemini-3.5-flash-lite' },
  { name: 'cloud-accurate', mode: 'only_in_cloud', model: 'gemini-3.8-flash' },
];

export async function runB(paths = PATHS, items: Truth[] = evalB.items) {
  if (typeof paths === 'string') paths = PATHS.filter(p => (paths as unknown as string).split(',').includes(p.name));
  const inf = new FirebaseAiLogicInference(getApp());
  const rows: Record<string, unknown>[] = [];
  for (const p of paths) for (const t of items) {
    const t0 = performance.now();
    let cards: Card[] | null = null, valid = true, error: string | undefined;
    if (!p.mode) {
      const k = keywordIcons(t.text, keywords);
      cards = k.icons.length ? [{ icon: k.icons[0], title: '', time: k.times[0] ?? null, place: k.place }] : null;
    } else {
      try {
        const task = t.lang === 'id' ? 'announcement_id' : 'announcement';
        const r = await Promise.race([new Promise<never>((_, rej) => setTimeout(() => rej(new Error('timeout')), 40_000)), inf.generate({ task, prompt: (pack.tasks as Record<string, string>)[task], text: t.text, schema: SCHEMAS.announcementCard, mode: p.mode, model: p.model })]);
        const v = parseAndValidate<{ cards: Card[] }>(r.raw, SCHEMAS.announcementCard);
        valid = v.valid; cards = v.value?.cards ?? null;
      } catch (e) { valid = false; error = String(e).slice(0, 300); }
    }
    rows.push({ path: p.name, id: t.id, lang: t.lang, ms: Math.round(performance.now() - t0), valid, error, ...scoreCards(t, cards) });
    console.log('[evalB]', p.name, t.id, JSON.stringify(rows.at(-1)));
    (globalThis as any).__evalProgress = rows.length;
  }
  const summary = paths.flatMap(p => ['en', 'id'].map(lang => {
    const r = rows.filter(x => x.path === p.name && x.lang === lang);
    const pct = (k: string) => Math.round((100 * r.filter(x => x[k]).length) / r.length);
    const ms = r.map(x => x.ms as number).sort((a, b) => a - b);
    return { path: p.name, lang, n: r.length, all: pct('all'), icon: pct('icon'), time: pct('time'), place: pct('place'), validJson: pct('valid'), medianMs: ms[Math.floor(ms.length / 2)] };
  }));
  return { summary, rows };
}
