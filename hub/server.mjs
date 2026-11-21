// Shelter Wi-Fi hub (D15): serves the built web app + public pages on the local hotspot. No AI, no deps.
// Run on the hub laptop: `npm --prefix web run build && node hub/server.mjs` then phones open http://<laptop-ip>:8787/#/public/news
import { createServer } from 'node:http';
import { readFile, writeFile } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';

const ROOT = new URL('../web/dist/', import.meta.url).pathname;
const STATE = new URL('./hub-state.json', import.meta.url).pathname;
const PORT = Number(process.env.PORT ?? 8787);
const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.json': 'application/json', '.webmanifest': 'application/manifest+json' };
let state = { news: [], help: [] };
try { state = JSON.parse(await readFile(STATE, 'utf8')); } catch { /* first run */ }
const save = () => writeFile(STATE, JSON.stringify(state)).catch(() => {});

async function body(req) {
  let s = '';
  for await (const c of req) { s += c; if (s.length > 64_000) throw new Error('too big'); }
  return JSON.parse(s);
}

createServer(async (req, res) => {
  const url = new URL(req.url, 'http://hub');
  try {
    if (url.pathname === '/api/news' && req.method === 'GET') return json(res, state.news);
    // Only the hub laptop itself may publish announcements.
    if (url.pathname === '/api/news' && req.method === 'POST' && isLocal(req)) { state.news = (await body(req)).slice(0, 50); save(); return json(res, { ok: true }); }
    if (url.pathname === '/api/help' && req.method === 'POST') {
      const h = await body(req);
      state.help.push({ kind: String(h.kind).slice(0, 40), spot: String(h.spot).slice(0, 40), at: Date.now() }); save();
      return json(res, { ok: true });
    }
    if (url.pathname === '/api/help' && req.method === 'GET' && isLocal(req)) return json(res, state.help);
    const file = normalize(join(ROOT, url.pathname === '/' ? 'index.html' : url.pathname));
    if (!file.startsWith(ROOT)) { res.writeHead(403).end(); return; }
    const data = await readFile(file).catch(() => readFile(join(ROOT, 'index.html')));
    res.writeHead(200, { 'content-type': TYPES[extname(file)] ?? 'application/octet-stream' }).end(data);
  } catch { res.writeHead(400).end(); }
}).listen(PORT, () => console.log(`Nobody Missing hub on http://0.0.0.0:${PORT}`));

function isLocal(req) { const a = req.socket.remoteAddress ?? ''; return a === '127.0.0.1' || a === '::1' || a === '::ffff:127.0.0.1'; }
function json(res, v) { res.writeHead(200, { 'content-type': 'application/json' }).end(JSON.stringify(v)); }
