// Ports: the only things features may depend on. One adapter per platform + test fakes (see adapters/adapterFactory.ts).
import type { Mode } from '../domain/router';
import type { Schema } from '../domain/validate';

export type ImageInput = { mimeType: 'image/jpeg' | 'image/png'; base64: string };
export type InferenceRequest = { task: string; prompt: string; text?: string; image?: ImageInput; schema: Schema; mode: Mode; model: string };
export type InferenceResult = { raw: string; source: 'device' | 'cloud' };

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

export type Adapters = { inference: InferencePort; sync: SyncPort; connectivity: ConnectivityPort; storage: StoragePort; clock: ClockPort; env: Env };
export type Env = 'prod' | 'emulator' | 'test';
