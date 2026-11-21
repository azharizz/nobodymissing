// Eval scorer (PRODUCT.md 7): compares model outputs with ground truth, per path (device vs cloud).
// Usage: node tools/eval/score.mjs <truth.json> <predictions.json>
//   truth:       { "items": [{ "id": "A01", "rows": [{ "name": "...", "age": 82, "sex": "M", "flags": [...] }] }] }
//   predictions: { "path": "device", "items": [{ "id": "A01", "rows": [...] , "ms": 950 }] }
// Prints field accuracy (name exact-as-written, age, sex, flags set) and median latency.
import { readFileSync } from 'node:fs';
const [truthFile, predFile] = process.argv.slice(2);
const truth = JSON.parse(readFileSync(truthFile, 'utf8')), pred = JSON.parse(readFileSync(predFile, 'utf8'));
const byId = new Map(pred.items.map(p => [p.id, p]));
const score = { name: [0, 0], age: [0, 0], sex: [0, 0], flags: [0, 0] };
const add = (k, ok) => { score[k][0] += ok ? 1 : 0; score[k][1]++; };
for (const t of truth.items) {
  const p = byId.get(t.id)?.rows ?? [];
  t.rows.forEach((row, i) => {
    const q = p[i] ?? {};
    add('name', q.name === row.name);
    add('age', (q.age ?? null) === (row.age ?? null));
    add('sex', (q.sex ?? null) === (row.sex ?? null));
    add('flags', JSON.stringify([...(q.flags ?? [])].sort()) === JSON.stringify([...(row.flags ?? [])].sort()));
  });
}
const ms = pred.items.map(p => p.ms).filter(Number.isFinite).sort((a, b) => a - b);
console.log(`path=${pred.path}`);
for (const [k, [ok, n]] of Object.entries(score)) console.log(`${k.padEnd(6)} ${(100 * ok / Math.max(n, 1)).toFixed(1)}%  (${ok}/${n})`);
if (ms.length) console.log(`median latency ${ms[Math.floor(ms.length / 2)]} ms`);
