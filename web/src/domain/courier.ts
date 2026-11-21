// QR courier (6.10 level 2): store-and-forward a queue export as numbered QR-sized chunks.
export function chunk(payload: string, size: number): string[] {
  const n = Math.max(1, Math.ceil(payload.length / size));
  return Array.from({ length: n }, (_, i) => `NM1|${i + 1}/${n}|${payload.slice(i * size, (i + 1) * size)}`);
}

export function join(chunks: string[]): string | null {
  const parts = new Map<number, string>();
  let total = -1;
  for (const c of chunks) {
    const m = /^NM1\|(\d+)\/(\d+)\|([\s\S]*)$/.exec(c);
    if (!m || (total >= 0 && +m[2] !== total)) return null;
    total = +m[2]; parts.set(+m[1], m[3]);
  }
  if (total < 1) return null;
  let out = '';
  for (let i = 1; i <= total; i++) { const p = parts.get(i); if (p == null) return null; out += p; }
  return out;
}
