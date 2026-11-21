// Offline announcement icons + times + place from a deterministic keyword list (D11). No AI, so it is exact.
export type Keywords = { icons: Record<string, string[]>; placeWords: string[] };

const pad = (n: number) => String(n).padStart(2, '0');

export function keywordIcons(text: string, kw: Keywords) {
  const lower = text.toLowerCase();
  const found: [number, string][] = [];
  for (const [icon, words] of Object.entries(kw.icons)) {
    let best = Infinity;
    for (const w of words) {
      const m = new RegExp(`(^|[^\\p{L}])${w}(?=[^\\p{L}]|$)`, 'u').exec(lower);
      if (m) best = Math.min(best, m.index + m[1].length);
    }
    if (best < Infinity) found.push([best, icon]);
  }
  const icons = found.sort((a, b) => a[0] - b[0]).map(f => f[1]);

  const times: [number, string][] = [];
  for (const m of lower.matchAll(/\b(\d{1,2})[.:](\d{2})\b/g)) {
    const h = +m[1], min = +m[2];
    if (h <= 23 && min <= 59) times.push([m.index!, `${pad(h)}:${pad(min)}`]);
  }
  for (const m of lower.matchAll(/\b(?:jam|pukul)\s+(\d{1,2})(?![.:]\d|\d)/g)) if (+m[1] <= 23) times.push([m.index!, `${pad(+m[1])}:00`]);
  for (const m of lower.matchAll(/\b(\d{1,2})\s*(am|pm)\b/g)) {
    const h = +m[1] % 12 + (m[2] === 'pm' ? 12 : 0);
    if (+m[1] <= 12) times.push([m.index!, `${pad(h)}:00`]);
  }
  const uniq = times.sort((a, b) => a[0] - b[0]).map(t => t[1]).filter((t, i, a) => a.indexOf(t) === i);

  return { icons, times: uniq, place: findPlace(text, kw.placeWords) };
}

function findPlace(text: string, placeWords: string[]): string | null {
  const words = text.split(/\s+/).filter(Boolean);
  const bare = (w: string) => w.toLowerCase().replace(/[,.!?;:]+$/, '');
  for (let i = 0; i < words.length; i++) {
    if (!placeWords.includes(bare(words[i]))) continue;
    const out: string[] = [];
    for (let j = i + 1; j < words.length; j++) {
      const w = words[j], b = bare(w);
      if (placeWords.includes(b) || b === 'jam' || b === 'pukul' || /^\d/.test(b)) break;
      out.push(w.replace(/[,.!?;:]+$/, ''));
      if (/[,.!?;:]$/.test(w)) break;
    }
    if (out.length) return out.join(' ');
  }
  return null;
}
