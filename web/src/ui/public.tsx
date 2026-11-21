// Big Screen (one auto-rotating page, readable from 10 m) + Public pages (News, Map, Help, Family) for deaf and elderly evacuees.
import { useState, useSyncExternalStore, type CSSProperties } from 'react';
import { regionPack as pack } from '../features/regionData';
import { nearestHigher } from '../domain/regionPack';
import { fmt } from './i18n';
import { Icon, Kit, QR, useAppState, useF, useT } from './common';
import { AnnouncementCards } from './staff';
import iconSet from '../../../shared/icons/icons.json';

// Module-level ticker (no useEffect): the Big Screen rotates every 9 s.
const SLIDE_MS = 9000;
let tick = 0;
const tickListeners = new Set<() => void>();
setInterval(() => { tick++; tickListeners.forEach(l => l()); }, SLIDE_MS);
const useTick = () => useSyncExternalStore(l => { tickListeners.add(l); return () => { tickListeners.delete(l); }; }, () => tick);

export function BigScreen() {
  const f = useF(), s = useAppState(), n = useTick(), t = useT();
  const news = f.views.news(), sch = f.views.schedule();
  const slides = ['news', 'meds', 'map', 'qr'] as const;
  const slide = slides[n % slides.length];
  const latest = news[0];
  const next = Object.entries(sch.counts)[0];
  return (
    <div className="big">
      <header className="big-head"><Kit mood="idle" size={120} /><div><b>{s.shelter.name}</b><span>{new Date().toLocaleDateString(s.lang === 'id' ? 'id-ID' : 'en-GB', { weekday: 'long', day: 'numeric', month: 'long' })}</span></div></header>
      <div className="big-stage" key={`${slide}-${n}`}>
        {slide === 'news' && (latest
          ? <div className="big-slide"><span className="eyebrow"><Icon name="announce" size={36} />{t.news}</span><AnnouncementCards a={latest} big /><p className="big-original">“{latest.text}”</p></div>
          : <div className="big-slide"><Kit mood="sleep" size={420} /><div className="large">{t.noNews}</div></div>)}
        {slide === 'meds' && <div className="big-slide"><span className="eyebrow"><Icon name="medicine" size={36} />{t.medicine}</span>
          <div className="huge">{next ? next[0] : '—'}</div><div className="large">{next ? `${t.nextRound} · ${next[1]}` : t.noMeds}</div></div>}
        {slide === 'map' && <div className="big-slide"><span className="eyebrow"><Icon name="map" size={36} />{t.map}</span><MiniMap big /></div>}
        {slide === 'qr' && <div className="big-slide qr-slide"><QR text={`${location.href.split('#')[0]}#/public/news`} size={380} label={t.news} />
          <div className="large">{[t.news, t.map, t.help, t.family].join(' · ')}</div></div>}
      </div>
      <div className="big-progress" key={`p${n}`} style={{ '--ms': `${SLIDE_MS}ms` } as CSSProperties} />
    </div>
  );
}

const PLACE_ICON: Record<string, string> = { medical: 'doctor', water: 'water', toilet: 'toilet', kitchen: 'food' };

function MiniMap({ big }: { big?: boolean }) {
  const s = useAppState(), f = useF(), t = useT();
  const sh = pack.shelters.find(x => x.id === s.shelter.id) ?? pack.shelters[0];
  const higher = nearestHigher(pack, sh.lat, sh.lng, f.rules.currentEvent, f.rules.higherMinGainM);
  const g = pack.grid, W = 320, cell = W / g.cols;
  const xy = (lat: number, lng: number) => [((lng - g.origin[0]) / g.step) * cell, W - ((lat - g.origin[1]) / g.step) * cell];
  return (
    <figure className={`map ${big ? 'big' : ''}`}>
      <svg viewBox={`0 0 ${W} ${W}`} role="img" aria-label={t.map}>
        {g.hand.map((row, r) => row.map((h, c) => <rect key={`${r}-${c}`} x={c * cell} y={W - (r + 1) * cell} width={cell} height={cell} rx={6}
          fill={h < 2 ? '#1D4E89' : h < 5 ? '#4F86C6' : h <= 10 ? '#A9C8EC' : '#EEF4FB'} stroke="#fff" strokeWidth={3} />))}
        {pack.places.map(p => { const [x, y] = xy(p.lat, p.lng); return (
          <g key={p.name} transform={`translate(${x - 11} ${y - 11})`}><circle cx={11} cy={11} r={13} fill="#fff" stroke="#14213D" strokeWidth={2} />
            <g transform="translate(2 2) scale(.75)"><path d={(iconPath(PLACE_ICON[p.kind]))} fill="none" stroke="#14213D" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" /></g></g>); })}
        {pack.shelters.map(p => { const [x, y] = xy(p.lat, p.lng); const me = p.id === sh.id; return (
          <g key={p.id}>{me && <circle className="map-ping" cx={x} cy={y} r={10} fill="#FF6B2C" />}
            <circle cx={x} cy={y} r={me ? 10 : 8} fill={me ? '#FF6B2C' : '#14213D'} stroke="#fff" strokeWidth={3} />
            <text x={x + 14} y={y + 5} fontSize="14" fontWeight="800" fill="#14213D" paintOrder="stroke" stroke="#fff" strokeWidth={4}>{p.id}</text></g>); })}
      </svg>
      <figcaption>
        {higher && <p className="higher"><Icon name="higher" size={20} /><span>{t.higherPlace}: <b>{higher.name}</b> · {higher.distanceM} m {higher.direction} · +{higher.handGain} m</span></p>}
        <p className="legend"><i className="l1" /> <i className="l2" /> <i className="l3" /> <i className="l4" /><small>{fmt(t.hazardNote, { event: f.rules.currentEvent })} {pack.source.hand}, {pack.source.date}.</small></p>
      </figcaption>
    </figure>
  );
}

const iconPath = (n: string) => (iconSet.icons as Record<string, string>)[n] ?? iconSet.icons.info;

type PTab = 'news' | 'map' | 'help' | 'family';
const PTABS: [PTab, string][] = [['news', 'announce'], ['map', 'map'], ['help', 'help'], ['family', 'family']];

export function Public({ tab, go }: { tab: PTab; go: (t: PTab) => void }) {
  const t = useT(), s = useAppState(), f = useF();
  const idx = PTABS.findIndex(([k]) => k === tab);
  return (
    <div className="app public">
      <header className="status"><Kit mood="idle" size={52} /><div className="status-title"><strong>{s.shelter.name}</strong><span>{t[tab]}</span></div>
        <div className="seg lang"><button aria-pressed={s.lang === 'en'} onClick={() => f.settings.setLang('en')}>EN</button><button aria-pressed={s.lang === 'id'} onClick={() => f.settings.setLang('id')}>ID</button></div></header>
      <main key={tab} className="view">
        {tab === 'news' && <News />}
        {tab === 'map' && <MiniMap />}
        {tab === 'help' && <HelpPage />}
        {tab === 'family' && <Family />}
      </main>
      <nav className="tabs" style={{ '--i': idx, '--n': PTABS.length } as CSSProperties}>
        <span className="tab-indicator" aria-hidden />
        {PTABS.map(([k, icon]) => <button key={k} aria-current={tab === k} onClick={() => go(k)}><Icon name={icon} size={24} /><span>{t[k]}</span></button>)}
      </nav>
    </div>
  );
}

function News() {
  const f = useF(), s = useAppState(), t = useT();
  const news = f.views.news();
  const speak = (text: string) => { const u = new SpeechSynthesisUtterance(text); u.lang = s.lang === 'id' ? 'id-ID' : 'en-US'; speechSynthesis.speak(u); };
  if (!news.length) return <div className="empty rise"><Kit mood="sleep" size={140} /><p>{t.noNews}</p></div>;
  return <>{news.map((a, i) => (
    <article key={a.id} className="newscard rise" style={{ '--d': i } as CSSProperties}>
      <AnnouncementCards a={a} />
      <div className="news-foot"><p className="muted">“{a.text}”</p><button className="btn icon-only" aria-label="Read aloud" onClick={() => speak(a.text)}><Icon name="speaker" /></button></div>
    </article>
  ))}</>;
}

function HelpPage() {
  const f = useF(), t = useT();
  const [spot, setSpot] = useState('');
  const [sent, setSent] = useState('');
  const kinds: [string, string][] = [['medicine', 'medicine'], ['sick', 'feeling sick'], ['mobility', 'need help moving'], ['baby', 'baby'], ['chat', 'talk in writing']];
  return (
    <section>
      <label className="search big-input"><Icon name="bed" /><input value={spot} placeholder="Bed 12 / Area B" aria-label="My spot" onChange={e => setSpot(e.target.value)} /></label>
      <div className="grid2">{kinds.map(([icon, k], i) => (
        <button key={k} className={`tile rise ${sent === k ? 'accent' : ''}`} style={{ '--d': i } as CSSProperties} onClick={() => { f.settings.help(k, spot || '?'); setSent(k); }}>
          <span className="tile-icon"><Icon name={icon} size={30} /></span><span className="lbl">{k}</span></button>
      ))}</div>
      {sent && <div className="notice ok rise" role="status"><Icon name="check" /><p>{t.help}: {sent}</p></div>}
    </section>
  );
}

function Family() {
  const t = useT(), f = useF();
  const [q, setQ] = useState('');
  const [res, setRes] = useState<Awaited<ReturnType<typeof f.family.search>> | null>(null);
  return (
    <section>
      <form className="search big-input" onSubmit={async e => { e.preventDefault(); setRes(await f.family.search(q)); }}>
        <Icon name="search" /><input value={q} placeholder={t.search} aria-label={t.search} onChange={e => setQ(e.target.value)} />
        <button className="btn primary" type="submit">{t.search}</button>
      </form>
      {res && <p className="muted"><Icon name={res.cloud ? 'cloud' : 'device'} size={15} />{res.cloud ? t.allShelters : t.thisShelterOnly}</p>}
      {res && res.hits.length === 0 && <p className="large">{t.notFound}</p>}
      {res?.hits.map((h, i) => <div key={i} className="card row rise" style={{ '--d': i } as CSSProperties}><span className="avatar">{h.name.slice(0, 1)}</span>
        <span className="grow"><b>{h.name}</b><small>{h.ageBand} · {h.shelterId}</small></span><span className={`pill-tag status-${h.status}`}>{h.status}</span></div>)}
    </section>
  );
}
