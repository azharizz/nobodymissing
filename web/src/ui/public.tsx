// Big Screen (one auto-rotating page) + Public pages (News, Map, Help, Family) for deaf and elderly evacuees.
import { useState, useSyncExternalStore } from 'react';
import { regionPack as pack } from '../features/regionData';
import { nearestHigher } from '../domain/regionPack';
import { ICON, QR, useAppState, useF, useT } from './common';
import { Kit } from './Kit';

// Module-level ticker (no useEffect): the Big Screen rotates every 8 s.
let tick = 0;
const tickListeners = new Set<() => void>();
setInterval(() => { tick++; tickListeners.forEach(l => l()); }, 8000);
const useTick = () => useSyncExternalStore(l => { tickListeners.add(l); return () => { tickListeners.delete(l); }; }, () => tick);

export function BigScreen() {
  const f = useF(), s = useAppState(), n = useTick();
  const news = f.views.news(), sch = f.views.schedule();
  const slides = ['news', 'meds', 'map', 'qr'] as const;
  const slide = slides[n % slides.length];
  const latest = news[0];
  const next = Object.entries(sch.counts)[0];
  return (
    <div className="big">
      {slide === 'news' && (latest ? (
        <div className="bigcard"><Kit pose="card" size={160} />
          {(latest.cards ?? [{ icon: latest.icons[0] ?? 'info', title: latest.text, time: latest.times[0], place: latest.place }]).slice(0, 2).map((c, i) => (
            <div key={i}><div className="huge">{ICON[c.icon] ?? 'ℹ️'} {c.title}{c.time ? ` · ${c.time}` : ''}</div>{c.place && <div className="large">📍 {c.place}</div>}</div>
          ))}
          <div className="muted">“{latest.text}”</div></div>
      ) : <div className="huge">{s.shelter.name}</div>)}
      {slide === 'meds' && <div className="bigcard"><div className="huge">💊 {next ? `${next[0]} · ${next[1]}` : '—'}</div><div className="large">Next medicine round. Please stay at your place.</div></div>}
      {slide === 'map' && <div className="bigcard"><MiniMap /></div>}
      {slide === 'qr' && <div className="bigcard"><QR text={`${location.href.split('#')[0]}#/public/news`} size={360} label="Scan for News, Map, Help, Family" /><div className="large">Scan for 📢 News · 🗺️ Map · 🙋 Help · 🔍 Family</div></div>}
    </div>
  );
}

function MiniMap() {
  const s = useAppState(), f = useF();
  const sh = pack.shelters.find(x => x.id === s.shelter.id) ?? pack.shelters[0];
  const higher = nearestHigher(pack, sh.lat, sh.lng, f.rules.currentEvent, f.rules.higherMinGainM);
  const g = pack.grid, W = 320, cell = W / g.cols;
  const xy = (lat: number, lng: number) => [((lng - g.origin[0]) / g.step) * cell, W - ((lat - g.origin[1]) / g.step) * cell];
  return (
    <figure>
      <svg viewBox={`0 0 ${W} ${W}`} width="100%" style={{ maxWidth: 480 }} role="img" aria-label="Shelter map">
        {g.hand.map((row, r) => row.map((h, c) => <rect key={`${r}-${c}`} x={c * cell} y={W - (r + 1) * cell} width={cell} height={cell} fill={h < 2 ? '#1f4e9c' : h < 5 ? '#6d9be0' : h <= 10 ? '#c8dbf5' : '#f3f6fa'} stroke="#fff" />))}
        {pack.places.map(p => { const [x, y] = xy(p.lat, p.lng); return <text key={p.name} x={x} y={y} fontSize="14">{({ medical: '🩺', water: '💧', toilet: '🚻', kitchen: '🍙' } as Record<string, string>)[p.kind]}</text>; })}
        {pack.shelters.map(p => { const [x, y] = xy(p.lat, p.lng); return <g key={p.id}><circle cx={x} cy={y} r="8" fill={p.id === sh.id ? '#f28c00' : '#222'} /><text x={x + 10} y={y + 4} fontSize="12" fontWeight="bold">{p.id}</text></g>; })}
      </svg>
      <figcaption>
        Darker blue = lower above the river (more likely to flood). {higher ? `Nearest higher place for ${f.rules.currentEvent}: ${higher.name}, ${higher.distanceM} m ${higher.direction}, +${higher.handGain} m.` : ''}
        <br /><small>Map data: {pack.source.hand}, {pack.source.date}. A map is a scenario, not live conditions.</small>
      </figcaption>
    </figure>
  );
}

type PTab = 'news' | 'map' | 'help' | 'family';
export function Public({ tab, go }: { tab: PTab; go: (t: PTab) => void }) {
  const t = useT(), s = useAppState(), f = useF();
  return (
    <div className="app public">
      <header className="status"><Kit size={36} /> <strong>{s.shelter.name}</strong>
        <span className="spacer" /><button onClick={() => f.settings.setLang(s.lang === 'en' ? 'id' : 'en')}>{s.lang === 'en' ? 'EN | id' : 'en | ID'}</button></header>
      <main>
        {tab === 'news' && <News />}
        {tab === 'map' && <MiniMap />}
        {tab === 'help' && <HelpPage />}
        {tab === 'family' && <Family />}
      </main>
      <nav className="tabs">{([['news', '📢'], ['map', '🗺️'], ['help', '🙋'], ['family', '🔍']] as [PTab, string][]).map(([k, i]) => <button key={k} aria-current={tab === k} onClick={() => go(k)}>{i}<br />{t[k]}</button>)}</nav>
    </div>
  );
}

function News() {
  const f = useF(), s = useAppState();
  const news = f.views.news();
  const speak = (text: string) => { const u = new SpeechSynthesisUtterance(text); u.lang = s.lang === 'id' ? 'id-ID' : 'en-US'; speechSynthesis.speak(u); };
  if (!news.length) return <p className="large">No announcements yet.</p>;
  return <>{news.map(a => (
    <article key={a.id} className="newscard">
      {(a.cards ?? [{ icon: a.icons[0] ?? 'info', title: a.text, time: a.times[0], place: a.place }]).map((c, i) => (
        <div key={i}><div className="huge">{ICON[c.icon] ?? 'ℹ️'} {c.title}{c.time ? ` · ${c.time}` : ''}</div>{c.place && <div className="large">📍 {c.place}</div>}</div>
      ))}
      <p className="muted">“{a.text}”</p>
      <button aria-label="Read aloud" onClick={() => speak(a.text)}>🔊</button>
    </article>
  ))}</>;
}

function HelpPage() {
  const f = useF();
  const [spot, setSpot] = useState('');
  const [sent, setSent] = useState('');
  const kinds: [string, string][] = [['💊', 'medicine'], ['🩺', 'feeling sick'], ['🚻', 'need help moving'], ['🍼', 'baby'], ['💬', 'talk in writing']];
  return (
    <section>
      <label className="large">📍 My spot <input value={spot} placeholder="Bed 12 / Area B" onChange={e => setSpot(e.target.value)} /></label>
      <div className="grid2">{kinds.map(([i, k]) => <button key={k} className="tile" onClick={() => { f.settings.help(k, spot || '?'); setSent(k); }}><span className="num">{i}</span>{k}</button>)}</div>
      {sent && <p className="notice large">✓ Staff can see your request: {sent}.</p>}
    </section>
  );
}

function Family() {
  const t = useT(), f = useF();
  const [q, setQ] = useState('');
  const [res, setRes] = useState<Awaited<ReturnType<typeof f.family.search>> | null>(null);
  return (
    <section>
      <input className="search large" value={q} placeholder="Name" onChange={e => setQ(e.target.value)} />
      <button className="main" onClick={async () => setRes(await f.family.search(q))}>🔍 {t.search}</button>
      {res && <p className="muted">{res.cloud ? t.allShelters : t.thisShelterOnly}</p>}
      {res && res.hits.length === 0 && <p className="large">{t.notFound}</p>}
      {res?.hits.map((h, i) => <div key={i} className="card large"><b>{h.name}</b> · {h.ageBand} · {h.shelterId} · {h.status}</div>)}
    </section>
  );
}
