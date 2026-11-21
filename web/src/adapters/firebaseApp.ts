// Firebase bootstrap: App Check (required for AI Logic since Nov 2 2026), silent Anonymous Auth (D14),
// Remote Config with bundled defaults (works offline), Firestore with offline persistence.
import { initializeApp, type FirebaseApp } from 'firebase/app';
import { initializeAppCheck, ReCaptchaEnterpriseProvider } from 'firebase/app-check';
import { connectAuthEmulator, getAuth, signInAnonymously } from 'firebase/auth';
import { connectFirestoreEmulator, doc, initializeFirestore, persistentLocalCache, setDoc, type Firestore } from 'firebase/firestore';
import { fetchAndActivate, getRemoteConfig, getValue } from 'firebase/remote-config';
import defaults from '../../../shared/rules/defaults.json';
import pack from '../../../shared/prompts/pack.json';
import seed from '../../../shared/fixtures/demo-shelter.json';

export type Rules = typeof defaults;
const env = import.meta.env;

export function hasFirebaseConfig() { return !!env.VITE_FIREBASE_API_KEY; }

export function initFirebase(emulator: boolean): { app: FirebaseApp; db: Firestore } {
  const app = initializeApp({
    apiKey: env.VITE_FIREBASE_API_KEY, authDomain: env.VITE_FIREBASE_AUTH_DOMAIN, projectId: env.VITE_FIREBASE_PROJECT_ID,
    storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET, messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID, appId: env.VITE_FIREBASE_APP_ID,
  });
  // Debug token for development and judges (PRODUCT.md 2.2). Register the printed token in the console.
  if (env.DEV || env.VITE_APPCHECK_DEBUG === 'true') (self as any).FIREBASE_APPCHECK_DEBUG_TOKEN = env.VITE_APPCHECK_DEBUG_TOKEN || true;
  initializeAppCheck(app, { provider: new ReCaptchaEnterpriseProvider(env.VITE_RECAPTCHA_ENTERPRISE_KEY ?? ''), isTokenAutoRefreshEnabled: true });
  const auth = getAuth(app);
  const db = initializeFirestore(app, { localCache: persistentLocalCache() });
  if (emulator) {
    connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
    connectFirestoreEmulator(db, '127.0.0.1', 8080);
  }
  // Join = role from the scanned QR (?join=SD3&role=medic), stored on the device. The hub device defaults to organizer.
  const q = new URLSearchParams(location.hash.split('?')[1] ?? '');
  const role = q.get('role') ?? localStorage.getItem('nm:role') ?? 'organizer';
  try { localStorage.setItem('nm:role', role); } catch { /* private window */ }
  signInAnonymously(auth)
    .then(c => setDoc(doc(db, 'shelters', q.get('join') ?? seed.shelter.id, 'members', c.user.uid), { role, joinedAt: Date.now() }))
    .catch(() => { /* offline or already a member: retried on next start */ });
  return { app, db };
}

/** Remote Config: in-code defaults first (offline), then fetched values when online. */
export async function loadRemoteRules(app: FirebaseApp): Promise<{ rules: Rules; prompts: Record<string, string> }> {
  const rc = getRemoteConfig(app);
  rc.defaultConfig = { rules: JSON.stringify(defaults), promptPack: JSON.stringify(pack) };
  rc.settings.minimumFetchIntervalMillis = import.meta.env.DEV ? 0 : 3_600_000;
  try { await fetchAndActivate(rc); } catch { /* offline: defaults */ }
  const parse = <T,>(k: string, fallback: T): T => { try { return JSON.parse(getValue(rc, k).asString()) as T; } catch { return fallback; } };
  return { rules: { ...defaults, ...parse('rules', defaults) }, prompts: parse('promptPack', pack).tasks };
}
