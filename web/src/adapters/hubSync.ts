import type { SyncPort } from '../ports';

/** Shelter Wi-Fi hub (D15): the local server in hub/ that phones reach over the hotspot with no internet. */
export class HubSync implements SyncPort {
  async put(path: string, data: object) { await fetch(`/api/${path}`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(data) }); }
  async list(col: string) { const r = await fetch(`/api/${col}`, { cache: 'no-store' }); return r.ok ? r.json() : []; }
  static async available() { try { return (await fetch('/api/news', { cache: 'no-store', signal: AbortSignal.timeout(1500) })).ok; } catch { return false; } }
}
