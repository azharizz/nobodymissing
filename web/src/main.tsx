// Startup work is module-level init (no useEffect): adapters -> Remote Config -> router -> features -> render.
import { createRoot } from 'react-dom/client';
import { createAdapters } from './adapters/adapterFactory';
import { createFeatures } from './features/featureFactory';
import type { Env } from './ports';
import { App } from './ui/App';
import { FeaturesCtx } from './ui/common';
import './ui/styles.css';

const env: Env = import.meta.env.VITE_USE_EMULATOR === 'true' ? 'emulator' : import.meta.env.VITE_FIREBASE_API_KEY ? 'prod' : 'demo';
const adapters = await createAdapters(env);
let remote: { rules?: any; prompts?: Record<string, string> } = {};
if (env !== 'demo') {
  const { loadRemoteRules } = await import('./adapters/firebaseApp');
  const { getApp } = await import('firebase/app');
  remote = await loadRemoteRules(getApp());
}
const features = createFeatures(adapters, remote.rules, remote.prompts);

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
addEventListener('online', () => features.scout.check());

createRoot(document.getElementById('root')!).render(<FeaturesCtx.Provider value={features}><App /></FeaturesCtx.Provider>);
