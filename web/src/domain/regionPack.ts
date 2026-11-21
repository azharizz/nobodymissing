// Region pack lookups (6.11b): plain geography, no AI. Never says "safe": only "higher" + "designated for this hazard".
export type Pack = {
  grid: { origin: number[]; step: number; cols: number; rows: number; hand: number[][] };
  shelters: { id: string; name: string; lat: number; lng: number; hazards: string[] }[];
};

export function handAt(pack: Pack, lat: number, lng: number): number | null {
  const g = pack.grid;
  const r = Math.floor((lat - g.origin[1]) / g.step), c = Math.floor((lng - g.origin[0]) / g.step);
  return r < 0 || c < 0 || r >= g.rows || c >= g.cols ? null : g.hand[r][c];
}

export function distanceM(a: number, b: number, c: number, d: number) {
  const R = 6371000, rad = Math.PI / 180, dp = (c - a) * rad, dl = (d - b) * rad;
  const h = Math.sin(dp / 2) ** 2 + Math.cos(a * rad) * Math.cos(c * rad) * Math.sin(dl / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

export function direction(a: number, b: number, c: number, d: number) {
  const rad = Math.PI / 180, dl = (d - b) * rad;
  const y = Math.sin(dl) * Math.cos(c * rad), x = Math.cos(a * rad) * Math.sin(c * rad) - Math.sin(a * rad) * Math.cos(c * rad) * Math.cos(dl);
  const deg = (Math.atan2(y, x) / rad + 360) % 360;
  return ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][Math.round(deg / 45) % 8];
}

/** Nearest shelter designated for this hazard that is at least minGain m higher above drainage (HAND). */
export function nearestHigher(pack: Pack, lat: number, lng: number, event: string, minGain: number) {
  const here = handAt(pack, lat, lng);
  if (here == null) return null;
  const best = pack.shelters
    .filter(s => s.hazards.includes(event))
    .map(s => ({ s, gain: (handAt(pack, s.lat, s.lng) ?? -Infinity) - here, dist: distanceM(lat, lng, s.lat, s.lng) }))
    .filter(x => x.gain >= minGain)
    .sort((a, b) => a.dist - b.dist)[0];
  return best ? { id: best.s.id, name: best.s.name, distanceM: Math.round(best.dist), direction: direction(lat, lng, best.s.lat, best.s.lng), handGain: Math.round(best.gain * 10) / 10 } : null;
}
