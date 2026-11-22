// Startup work is module-level init (no useEffect): adapters -> Remote Config -> router -> features -> render.
import { createRoot } from 'react-dom/client';
import { createAdapters } from './adapters/adapterFactory';
import { createFeatures } from './features/featureFactory';
import type { Env } from './ports';
import { App } from './ui/App';
import { demoState, refreshDemoLocations } from './demo/seed';
import { FeaturesCtx, loadQrLibs } from './ui/common';
import './ui/styles.css';

const env: Env = import.meta.env.VITE_USE_EMULATOR === 'true' ? 'emulator' : import.meta.env.VITE_FIREBASE_API_KEY ? 'prod' : 'demo';
// Join QR: #/staff/now?join=SD3&role=medic -> this device belongs to that shelter.
const join = new URLSearchParams(location.hash.split('?')[1] ?? '').get('join');
const savedShelter = (() => { try { return JSON.parse(localStorage.getItem('nm:state') ?? 'null')?.shelter?.id as string | undefined; } catch { return undefined; } })();
const adapters = await createAdapters(env, join ?? savedShelter ?? (env === 'demo' ? 'SD3' : 'shelter-1'));
let remote: { rules?: any; prompts?: Record<string, string> } = {};
if (env !== 'demo') {
  const { loadRemoteRules } = await import('./adapters/firebaseApp');
  const { getApp } = await import('firebase/app');
  remote = await loadRemoteRules(getApp());
}
const features = createFeatures(adapters, remote.rules, remote.prompts, env === 'demo' ? demoState : undefined);
features.store.set(s => ({ ...s, field: refreshDemoLocations(s.field) }));
if (import.meta.env.DEV) (window as any).nm = features;
void features.router.warm(); // first question fast: model ready before anyone types // dev-only handle for debugging the outbox
if (join && features.store.get().shelter.id !== join) features.store.set(s => ({ ...s, shelter: { id: join, name: `Shelter ${join}` } }));

// Signal Scout loop: real checks on an interval (Remote Config), longer when the battery is low.
let lastAlert = false, lastNews = 0;
features.store.subscribe(() => {
  const s = features.store.get();
  if (s.scoutAlert && !lastAlert) {
    navigator.vibrate?.([400, 200, 400, 200, 400]);
    try { const ctx = new AudioContext(), o = ctx.createOscillator(); o.connect(ctx.destination); o.frequency.value = 880; o.start(); o.stop(ctx.currentTime + 0.6); } catch { /* no audio */ }
  }
  lastAlert = s.scoutAlert;
  // Deaf users notice a new card without sound: the public screen flashes and the phone vibrates.
  const news = s.announcements.filter(a => a.shown).length;
  if (news > lastNews && location.hash.startsWith('#/public')) { document.body.classList.add('flash'); navigator.vibrate?.(300); setTimeout(() => document.body.classList.remove('flash'), 2000); }
  lastNews = news;
});
const battery = await (navigator as any).getBattery?.().catch(() => null);
const scoutLoop = () => {
  features.scout.check();
  const low = battery && !battery.charging && battery.level * 100 <= features.rules.lowBatteryPercent;
  setTimeout(scoutLoop, 1000 * (low ? features.rules.scout.lowBatteryIntervalSec : features.rules.scout.intervalSec));
};
scoutLoop();
// Batch mode: read waiting photos when the device starts charging (generator hours).
battery?.addEventListener?.('chargingchange', () => { if (battery.charging) features.roster.processBatch(); });
addEventListener('online', () => features.scout.check());

// Shelter Wi-Fi: the hub laptop publishes shown cards to the local server; phones on the hotspot read them.
const { HubSync } = await import('./adapters/hubSync');
if (await HubSync.available()) {
  const hub = new HubSync();
  if (location.hash.startsWith('#/public')) {
    const pull = async () => {
      const news = await hub.list('news').catch(() => []) as any[];
      if (news.length) features.store.set(s => ({ ...s, announcements: news }));
      const help = features.store.get().help.filter(h => !h.done && !(h as any).sent);
      for (const h of help) { await hub.put('help', h).catch(() => {}); features.store.set(s => ({ ...s, help: s.help.map(x => x.id === h.id ? { ...x, sent: true } as any : x) })); }
    };
    pull(); setInterval(pull, 15000);
  } else {
    let last = '';
    const push = () => { const news = JSON.stringify(features.store.get().announcements.filter(a => a.shown)); if (news !== last) { last = news; hub.put('news', JSON.parse(news)).catch(() => {}); } };
    features.store.subscribe(push); push();
    setInterval(async () => {
      const help = await hub.list('help').catch(() => []) as any[];
      const have = new Set(features.store.get().help.map(h => h.at + h.kind));
      const fresh = help.filter(h => !have.has(h.at + h.kind)).map(h => ({ id: crypto.randomUUID(), kind: h.kind, spot: h.spot, at: h.at, done: false }));
      if (fresh.length) features.store.set(s => ({ ...s, help: [...s.help, ...fresh] }));
    }, 15000);
  }
}

createRoot(document.getElementById('root')!).render(<FeaturesCtx.Provider value={features}><App /></FeaturesCtx.Provider>);
// After first paint: fetch the QR libraries so they are cached for offline scanning.
setTimeout(() => { loadQrLibs().catch(() => {}); }, 1500);
