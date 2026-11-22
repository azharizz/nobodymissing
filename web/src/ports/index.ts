// Ports: the only things features may depend on. One adapter per platform + test fakes (see adapters/adapterFactory.ts).
import type { Mode } from '../domain/router';
import type { Schema } from '../domain/validate';

export type ImageInput = { mimeType: 'image/jpeg' | 'image/png'; base64: string };
export type GroundingPlace = { title?: string; uri?: string; placeId?: string };
/** grounding 'maps' = Grounding with Google Maps (cloud only), near `location` (already rounded by the caller). */
export type InferenceRequest = { task: string; prompt: string; text?: string; image?: ImageInput; schema: Schema; mode: Mode; model: string; grounding?: 'maps' | 'search'; location?: { lat: number; lng: number } };
export type InferenceResult = { raw: string; source: 'device' | 'cloud'; places?: GroundingPlace[] };

/** Firebase AI Logic only (D11). onDeviceStatus: 'no_api' | 'available' | 'downloadable' | 'downloading' | 'unavailable'. */
export interface InferencePort {
  onDeviceStatus(): Promise<string>;
  generate(r: InferenceRequest): Promise<InferenceResult>;
}

/** Cloud sync (Firestore). The only network calls besides inference. */
export interface SyncPort {
  put(path: string, data: object): Promise<void>;
  list(collection: string): Promise<Record<string, unknown>[]>;
}

/** Real internet check, never the OS "connected" flag (6.10). */
export interface ConnectivityPort { check(): Promise<boolean> }

export interface StoragePort {
  load<T>(key: string): T | undefined;
  save(key: string, value: unknown): void;
}

export interface ClockPort { now(): number }

export type Adapters = {
  inference: InferencePort; sync: SyncPort; connectivity: ConnectivityPort; storage: StoragePort; clock: ClockPort; env: Env;
  /** Starts the on-device model download; must run from a user gesture (web). */
  prepareDevice?: () => Promise<string>;
};
/** prod = Firebase; emulator = Firebase Emulator Suite; test = fakes; demo = fakes with canned AI (no Firebase project). */
export type Env = 'prod' | 'emulator' | 'test' | 'demo';
