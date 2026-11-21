import type { Adapters } from '../ports';
import type { Rules } from '../adapters/firebaseApp';
import type { InferenceRouter } from './inferenceRouter';
import type { Store } from './state';

/** What FeatureFactory hands each feature: ports + router + rules. Never an adapter. */
export type Deps = { store: Store; router: InferenceRouter; rules: Rules } & Pick<Adapters, 'sync' | 'connectivity' | 'clock'>;
export const uid = () => crypto.randomUUID();
export const online = (d: Deps) => d.store.get().signal === 'back';
