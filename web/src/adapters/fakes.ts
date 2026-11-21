// Test fakes for every port. Also used by the "demo" build when no Firebase config is present.
import type { ConnectivityPort, InferencePort, InferenceRequest, InferenceResult, StoragePort, SyncPort } from '../ports';

export class FakeInference implements InferencePort {
  calls: InferenceRequest[] = [];
  constructor(
    public status = 'available',
    public reply: (r: InferenceRequest) => Promise<InferenceResult> | InferenceResult = r => ({ raw: '{}', source: r.mode === 'only_in_cloud' || r.mode === 'prefer_in_cloud' ? 'cloud' : 'device' }),
  ) {}
  async onDeviceStatus() { return this.status; }
  async generate(r: InferenceRequest) { this.calls.push(r); return this.reply(r); }
}

export class MemorySync implements SyncPort {
  docs = new Map<string, object>();
  fail = false;
  async put(path: string, data: object) { if (this.fail) throw new Error('offline'); this.docs.set(path, data); }
  async list(collection: string) {
    return [...this.docs].filter(([k]) => k.startsWith(collection + '/')).map(([, v]) => v as Record<string, unknown>);
  }
}

export class FakeConnectivity implements ConnectivityPort {
  constructor(public online = true) {}
  async check() { return this.online; }
}

export class MemoryStorage implements StoragePort {
  m = new Map<string, unknown>();
  load<T>(k: string) { return this.m.get(k) as T | undefined; }
  save(k: string, v: unknown) { this.m.set(k, v); }
}

