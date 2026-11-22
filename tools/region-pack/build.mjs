// Region pack builder (PRODUCT.md 6.13). Real open data, no GDAL needed:
//   HAND: Global HAND 30 m (glo-30-hand, AWS Open Data, derived from Copernicus GLO-30) read as a COG window.
//   Shelter candidates + places: OpenStreetMap via Overpass (ODbL).
// Usage: node build.mjs <id> <west> <south> <east> <north> <out.json>
import { fromUrl } from 'geotiff';
import { writeFileSync } from 'node:fs';

const [id, w, s, e, n, out] = [process.argv[2], ...process.argv.slice(3, 7).map(Number), process.argv[7]];
const STEP = 0.001; // ~110 m cells: fine enough to follow the river, small enough to bundle (~20 KB)
const tile = (lat, lng) => `https://glo-30-hand.s3.amazonaws.com/v1/2021/Copernicus_DSM_COG_10_${lat >= 0 ? 'N' : 'S'}${String(Math.abs(Math.floor(lat))).padStart(2, '0')}_00_${lng >= 0 ? 'E' : 'W'}${String(Math.abs(Math.floor(lng))).padStart(3, '0')}_00_HAND.tif`;

// 1. HAND grid (one 1x1 degree tile covers a town; ponytail: multi-tile bboxes not handled, add when a pack crosses a degree line)
const img = await (await fromUrl(tile(s, w))).getImage();
const [ox, oy] = img.getOrigin(), [rx, ry] = img.getResolution();
const px = (lng, lat) => [Math.floor((lng - ox) / rx), Math.floor((lat - oy) / ry)];
const [x0, y0] = px(w, n), [x1, y1] = px(e, s);
const [r] = await img.readRasters({ window: [x0, y0, x1, y1] });
const W = x1 - x0;
const cols = Math.round((e - w) / STEP), rows = Math.round((n - s) / STEP);
const hand = [];
for (let row = 0; row < rows; row++) { // row 0 = southern edge
  const line = [];
  for (let c = 0; c < cols; c++) {
    // median of the 30 m pixels inside the cell (robust to single noisy pixels)
    const [a, b] = px(w + c * STEP, s + (row + 1) * STEP), [a2, b2] = px(w + (c + 1) * STEP, s + row * STEP);
    const vals = [];
    for (let y = b; y < b2; y++) for (let x = a; x < a2; x++) { const v = r[(y - y0) * W + (x - x0)]; if (v > -1 && v < 1000) vals.push(v); }
    vals.sort((p, q) => p - q);
    line.push(vals.length ? Math.round(vals[vals.length >> 1] * 10) / 10 : 99);
  }
  hand.push(line);
}
const handAt = (lat, lng) => { const rr = Math.floor((lat - s) / STEP), cc = Math.floor((lng - w) / STEP); return hand[rr]?.[cc] ?? null; };

// 2. Named schools, places of worship and hospitals from OSM = shelter candidates (an organizer confirms the real list)
const q = `[out:json][timeout:50];(nwr["amenity"~"school|place_of_worship|hospital|clinic"]["name"](${s},${w},${n},${e}););out center tags;`;
const osm = await (await fetch('https://overpass-api.de/api/interpreter', { method: 'POST', headers: { 'User-Agent': 'NobodyMissing-RegionPack/1.0' }, body: new URLSearchParams({ data: q }) })).json();
const shelters = osm.elements.map(el => {
  const lat = el.lat ?? el.center.lat, lng = el.lon ?? el.center.lon, h = handAt(lat, lng);
  return { id: `osm-${el.type[0]}${el.id}`, name: el.tags.name, kind: el.tags.amenity, lat: +lat.toFixed(5), lng: +lng.toFixed(5), hand: h,
    // Flood: only places well above the nearest drainage are candidates. Earthquake: any named public building.
    hazards: [...(h != null && h >= 5 ? ['flood'] : []), 'earthquake'] };
}).filter(x => x.hand != null);

writeFileSync(out, JSON.stringify({
  _notice: 'Region pack built from real open data by tools/region-pack/build.mjs. HAND = height above nearest drainage (m). Shelter list = OSM candidates; the organizer confirms which are open. Never says "safe": only "higher".',
  id, name: 'Kuala Simpang, Aceh Tamiang', bbox: [w, s, e, n],
  source: { hand: 'Global HAND 30 m (glo-30-hand), derived from Copernicus GLO-30 DEM © DLR e.V. 2010-2014 and © Airbus 2014-2018', osm: '© OpenStreetMap contributors (ODbL)', date: new Date().toISOString().slice(0, 10) },
  grid: { origin: [w, s], step: STEP, cols, rows, hand },
  shelters,
}));
console.log(`${rows}x${cols} HAND cells, ${shelters.length} shelter candidates (${shelters.filter(x => x.hazards.includes('flood')).length} flood-capable) -> ${out}`);
