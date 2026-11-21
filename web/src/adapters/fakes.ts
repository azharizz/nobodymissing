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

/** Canned replies so the app is usable with no Firebase project (demo mode; badges still say which path). */
export function demoReply(r: InferenceRequest): InferenceResult {
  const source = r.mode === 'only_in_cloud' || r.mode === 'prefer_in_cloud' ? 'cloud' : 'device';
  const t = r.text ?? '';
  const raw: Record<string, string> = {
    roster_read: JSON.stringify({ rows: [
      { name: 'Teuku Hasballah', age: 79, sex: 'M', flags: ['hard_of_hearing', 'wheelchair'], notes: 'tidak bisa dengar, pakai kursi roda', confidence: 0.86 },
      { name: 'Cut Nurlaila', age: 34, sex: 'F', flags: ['pregnant'], notes: null, confidence: 0.93 },
      { name: 'M. Rizki', age: 7, sex: 'M', flags: [], notes: null, confidence: 0.62 } ] }),
    care_card_read: JSON.stringify({ meds: [
      { nameAsWritten: 'Amlodipin 5 mg', strength: '5 mg', doseAsWritten: '1 x 1 tablet pagi', timesPerDay: 1, timing: 'after_meal', qtyLeft: 2, confidence: 0.88 },
      { nameAsWritten: 'Lantus', strength: '100 units/ml', doseAsWritten: '10 unit malam', timesPerDay: 1, timing: 'bedtime', qtyLeft: 1, confidence: 0.9 } ], conditions: ['hypertension', 'diabetes'], allergies: [] }),
    field_report: JSON.stringify({ people: [{ name: t.split(',')[0].slice(0, 40) || 'Unknown', age: Number(/\b(\d{2})\b/.exec(t)?.[1]) || null, mobility: /can't walk|tidak bisa jalan|bedridden/i.test(t) ? 3 : 1, medicalNeeds: /insulin/i.test(t) ? ['insulin'] : [], waterAsReported: /knee/i.test(t) ? 'knee-high' : null }] }),
    announcement: JSON.stringify({ cards: [{ icon: /water|air/i.test(t) ? 'water' : /medic|obat/i.test(t) ? 'medicine' : 'food', title: t.split(/[.,!]/)[0].slice(0, 30), time: /(\d{1,2})[.:](\d{2})/.exec(t)?.[0].replace('.', ':') ?? null, place: null, detail: null }] }),
    translate: JSON.stringify({ text: t }),
    supply_request: JSON.stringify({ items: [], note: 'Demo note' }),
  };
  raw.announcement_id = raw.announcement;
  return { raw: raw[r.task] ?? '{}', source };
}
