// Offline shell: cache-first for same-origin GETs so the hub opens with no signal. Firebase calls are never cached.
const CACHE = 'nm-v3';
self.addEventListener('install', e => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then(c => c.addAll(['/', '/index.html', '/kit.svg', '/manifest.webmanifest']))); });
self.addEventListener('activate', e => e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE && k !== 'nm-tiles').map(k => caches.delete(k)))).then(() => self.clients.claim())));
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  // OpenStreetMap tiles: cache-first so the map still shows with no signal (only tiles this device has seen; no bulk download).
  if (u.hostname === 'tile.openstreetmap.org') {
    e.respondWith(caches.open('nm-tiles').then(async c => (await c.match(e.request)) ?? fetch(e.request).then(r => { c.put(e.request, r.clone()); return r; })));
    return;
  }
  if (e.request.method !== 'GET' || u.origin !== location.origin || u.pathname.startsWith('/api/')) return;
  e.respondWith(caches.open(CACHE).then(async c => {
    try { const r = await fetch(e.request); if (r.ok) c.put(e.request, r.clone()); return r; }
    catch { return (await c.match(e.request)) ?? (await c.match('/index.html')); }
  }));
});
