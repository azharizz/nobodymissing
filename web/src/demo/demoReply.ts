// DEMO ONLY: canned AI replies for running the app with no Firebase project. Badges still say which path answered.
import type { InferenceRequest, InferenceResult } from '../ports';

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
