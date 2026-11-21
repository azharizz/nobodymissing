// Offline shell: cache-first for same-origin GETs so the hub opens with no signal. Firebase calls are never cached.
const CACHE = 'nm-v2';
self.addEventListener('install', e => { self.skipWaiting(); e.waitUntil(caches.open(CACHE).then(c => c.addAll(['/', '/index.html', '/kit.svg', '/manifest.webmanifest']))); });
self.addEventListener('activate', e => e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())));
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET' || u.origin !== location.origin || u.pathname.startsWith('/api/')) return;
  e.respondWith(caches.open(CACHE).then(async c => {
    try { const r = await fetch(e.request); if (r.ok) c.put(e.request, r.clone()); return r; }
    catch { return (await c.match(e.request)) ?? (await c.match('/index.html')); }
  }));
});
