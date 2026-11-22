// Photo eval: synthetic handwritten lists (tools/eval/make-sheets.mjs), read on the device vs in the cloud, through Firebase AI Logic.
// Usage (dev server, console): const m = await import('/src/eval/runPhotos.ts'); await m.runPhotos()
import { getApp } from 'firebase/app';
import pack from '../../../shared/prompts/pack.json';
import { FirebaseAiLogicInference } from '../adapters/firebaseAiLogic';
import { normalizeName } from '../domain/dedupe';
import { parseAndValidate, SCHEMAS } from '../domain/validate';
import type { Mode } from '../domain/router';

type Truth = { id: string; font: string; rows: { name: string; age: number; flags: string[] }[] };
type Row = { name: string; age?: number | null; flags?: string[] };
const PATHS: { name: string; mode: Mode; model: string }[] = [
  { name: 'device', mode: 'only_on_device', model: 'gemini-3.5-flash-lite' },
  { name: 'cloud', mode: 'only_in_cloud', model: 'gemini-3.5-flash-lite' },
];

/** Same downscale as the app's camera (1280 px JPEG). */
async function load(id: string) {
  const img = new Image(); img.src = `/eval/sheets/${id}.jpg`; await img.decode();
  const s = Math.min(1, 1280 / Math.max(img.width, img.height)), c = document.createElement('canvas');
  c.width = Math.round(img.width * s); c.height = Math.round(img.height * s); c.getContext('2d')!.drawImage(img, 0, 0, c.width, c.height);
  return { mimeType: 'image/jpeg' as const, base64: c.toDataURL('image/jpeg', 0.85).split(',')[1] };
}

/** Per person in the truth: is there a read row with the same name (normalised), and is the age / are the flags right? */
export function scoreSheet(truth: Truth['rows'], read: Row[]) {
  let names = 0, ages = 0, flags = 0;
  for (const t of truth) {
    const r = read.find(x => normalizeName(x.name) === normalizeName(t.name));
    if (!r) continue;
    names++;
    if (r.age === t.age) ages++;
    if (t.flags.every(f => r.flags?.includes(f))) flags++;
  }
  return { people: truth.length, names, ages, flags, extra: Math.max(0, read.length - truth.length) };
}

export async function runPhotos(paths = PATHS) {
  const truth: Truth[] = (await (await fetch('/eval/sheets/truth.json')).json()).sheets;
  const rows: Record<string, unknown>[] = [];
  const inf = new FirebaseAiLogicInference(getApp());
  for (const p of paths) for (const t of truth) {
    const image = await load(t.id), t0 = performance.now();
    let read: Row[] = [], error: string | undefined;
    try {
      const once = () => Promise.race([new Promise<never>((_, rej) => setTimeout(() => rej(new Error('timeout 90s')), 90_000)),
        inf.generate({ task: 'roster_read', prompt: pack.tasks.roster_read, image, schema: SCHEMAS.roster, mode: p.mode, model: p.model })]);
      // The free cloud sometimes says "high demand" (500) or "too many requests" (429): wait and retry, up to 3 tries.
      let r; for (let a = 0; ; a++) { try { r = await once(); break; } catch (e) { if (a >= 2 || !/\[(500|503|429) \]/.test(String(e))) throw e; await new Promise(res => setTimeout(res, 8000 * (a + 1))); } }
      const v = parseAndValidate<{ rows: Row[] }>(r.raw, SCHEMAS.roster);
      read = v.value?.rows ?? [];
      if (!v.valid) error = 'invalid: ' + v.errors.slice(0, 2).join('; ');
    } catch (e) { error = String(e).slice(0, 200); }
    rows.push({ path: p.name, id: t.id, font: t.font, ms: Math.round(performance.now() - t0), error, ...scoreSheet(t.rows, read) });
    console.log('[photos]', JSON.stringify(rows.at(-1)));
    (globalThis as any).__photoProgress = rows.length;
  }
  const summary = paths.map(p => {
    const r = rows.filter(x => x.path === p.name), sum = (k: string) => r.reduce((a, x) => a + (x[k] as number), 0);
    const ms = r.filter(x => !x.error).map(x => x.ms as number).sort((a, b) => a - b);
    const pct = (k: string) => Math.round((100 * sum(k)) / sum('people'));
    return { path: p.name, sheets: r.length, people: sum('people'), names: pct('names'), ages: pct('ages'), flags: pct('flags'), failed: r.filter(x => x.error).length, medianMs: ms[Math.floor(ms.length / 2)] ?? null };
  });
  return { summary, rows };
}
