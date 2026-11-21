// AdapterFactory: the only place adapters are constructed. Features get ports, never adapters.
import type { Adapters, Env } from '../ports';
import { FakeConnectivity, FakeInference, MemoryStorage, MemorySync } from './fakes';
import { FetchConnectivity, LocalStorage } from './browser';

export async function createAdapters(env: Env): Promise<Adapters & { prepareDevice?: () => Promise<string>; app?: unknown }> {
  const clock = { now: () => Date.now() };
  if (env === 'test') {
    return { env, clock, inference: new FakeInference(), sync: new MemorySync(), connectivity: new FakeConnectivity(), storage: new MemoryStorage() };
  }
  // Lazy imports keep the Firebase SDK out of the test bundle.
  const { initFirebase } = await import('./firebaseApp');
  const { FirebaseAiLogicInference } = await import('./firebaseAiLogic');
  const { FirestoreSync } = await import('./firestoreSync');
  const { app, db } = initFirebase(env === 'emulator');
  const inference = new FirebaseAiLogicInference(app);
  return {
    env, clock, app, inference, prepareDevice: () => inference.prepareDevice(),
    sync: new FirestoreSync(db), connectivity: new FetchConnectivity(), storage: new LocalStorage(),
  };
}
