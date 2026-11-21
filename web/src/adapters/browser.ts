import type { ConnectivityPort, StoragePort } from '../ports';

/** Real check: a tiny no-cache request with a short timeout. navigator.onLine is not trusted (6.10). */
export class FetchConnectivity implements ConnectivityPort {
  constructor(private url = 'https://firestore.googleapis.com/', private timeoutMs = 4000) {}
  async check() {
    try {
      await fetch(this.url, { method: 'HEAD', mode: 'no-cors', cache: 'no-store', signal: AbortSignal.timeout(this.timeoutMs) });
      return true;
    } catch { return false; }
  }
}

export class LocalStorage implements StoragePort {
  load<T>(k: string) { try { const v = localStorage.getItem(`nm:${k}`); return v == null ? undefined : (JSON.parse(v) as T); } catch { return undefined; } }
  save(k: string, v: unknown) { try { localStorage.setItem(`nm:${k}`, JSON.stringify(v)); } catch { /* storage full or blocked */ } }
}
