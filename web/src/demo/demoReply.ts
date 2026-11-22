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
    med_check: JSON.stringify({ items: [
      { name: 'insulin glargine', generic: 'insulin glargine', alsoKnownAs: ['Lantus', 'Basaglar'], swaps: ['insulin detemir', 'NPH insulin'], cantMiss: true, note: 'Keep cool; never skip.' },
      { name: 'amlodipine', generic: 'amlodipine', alsoKnownAs: ['Norvasc', 'Tensivask'], swaps: ['nifedipine (long-acting)'], cantMiss: false, note: null } ],
      interactions: [] }),
    find_open: JSON.stringify({ places: [{ name: 'Apotek Sehat Keluarga', kind: 'pharmacy', distanceKm: 1.2, openNow: true, hours: 'until 21:00', note: null }, { name: 'Puskesmas Kota (demo)', kind: 'health post', distanceKm: 2.4, openNow: true, hours: '24 hours', note: null }] }),
    help_triage: JSON.stringify({ kind: /insulin|obat|medic/i.test(t) ? 'medicine' : /sick|sakit|demam|fever|pain|nyeri/i.test(t) ? 'sick' : 'other', urgent: /chest|dada|breath|napas/i.test(t), summary: t.slice(0, 80) }),
    family_query: JSON.stringify({ name: t.replace(/^(my |nenek |kakek |ibu |bapak |grandmother |grandfather |saya )+/gi, '').split(/[,.]/)[0].replace(/\b(saya|my)\b/gi, '').trim().slice(0, 60) || t.slice(0, 60), age: Number(/(\d{2})/.exec(t)?.[1]) || null, place: /(?:from|dari) ([A-Za-z ]+)/i.exec(t)?.[1]?.trim() ?? null }),
    name_variants: JSON.stringify({ variants: t.includes(' ') ? [t.replace(' ', '')] : t.length > 6 ? [t.slice(0, 3) + ' ' + t.slice(3)] : [] }),
    warning_plain: JSON.stringify({ hazard: /banjir|flood/i.test(t) ? 'flood' : 'other', level: /segera|evakuasi|evacuate/i.test(t) ? 'go_now' : 'prepare', plain: 'The river is rising fast. Leave the riverside now and go to higher ground.', action: 'Go to higher ground now' }),
    warning_latest: JSON.stringify({ hazard: 'flood', level: 'go_now', plain: 'BPBD says the Tamiang river is still rising. People near the river must leave now.', action: 'Go to higher ground', source: 'BPBD Aceh Tamiang (demo)', issuedAt: '07:30' }),
    pickup_request: JSON.stringify({ who: t.split(/[,.]/)[0].slice(0, 40), mobility: /tidak bisa jalan|can't walk|bedridden/i.test(t) ? 3 : /kursi roda|wheelchair/i.test(t) ? 2 : 1, needs: /insulin/i.test(t) ? ['insulin'] : [], people: 1, note: null }),
    checkin_parse: JSON.stringify({ name: t.split(/[,\d]/)[0].trim().slice(0, 60), age: Number(/\b(\d{1,3})\b/.exec(t)?.[1]) || null, flags: /dengar|hearing/i.test(t) ? ['hard_of_hearing'] : [] }),
    ask_kit: (() => {
      // Demo: pick the notice sharing a word with the question (the real model reads them in any language).
      const words = t.toLowerCase().split(/\W+/).filter(w => w.length > 3 && !['when', 'what', 'where', 'does', 'will', 'there'].includes(w));
      const hit = [...r.prompt.matchAll(/id=(\S+) text=([^\n]*)/g)].find(m => words.some(w => m[2].toLowerCase().includes(w)));
      return JSON.stringify(hit ? { cardId: hit[1], answer: hit[2].replace(/ \(said:.*$/, '') } : { cardId: null, answer: 'Please ask a volunteer.' });
    })(),
  };
  raw.announcement_id = raw.announcement;
  // Demo grounding: the canned places come back as if Google Maps returned them.
  const places = r.task === 'warning_latest' ? [{ title: 'BPBD Aceh Tamiang (demo)', uri: 'https://bpbd.acehtamiangkab.go.id/' }] : r.task === 'find_open' ? [{ title: 'Apotek Sehat Keluarga', uri: 'https://maps.google.com/?q=Apotek+Sehat+Keluarga' }, { title: 'Puskesmas Kota (demo)', uri: 'https://maps.google.com/?q=Puskesmas' }] : undefined;
  return { raw: raw[r.task] ?? '{}', source, places };
}
