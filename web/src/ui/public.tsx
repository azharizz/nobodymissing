// Evacuee app (elderly and deaf first: huge type, read aloud, dark text on soft colour) + the Big Screen for the hall TV.
import { useState, useSyncExternalStore, type CSSProperties } from 'react';
import { regionPack as pack } from '../features/regionData';
import { distanceM, nearestHigher } from '../domain/regionPack';
import type { Announcement } from '../features/state';
import { fmt } from './i18n';
import { Badge, cardIcon, greeting, Icon, Kit, MicButton, navigate, QR, SayButton, ShelterMap, useAppState, useF, useT } from './common';

const go = (p: string) => navigate(`#/public/${p}`);
const PLACE_ICON: Record<string, string> = { medical: 'doctor', water: 'water', toilet: 'toilet', kitchen: 'food' };
const PLACE_WORD: Record<string, 'doctor' | 'water' | 'toilet' | 'kitchen'> = { medical: 'doctor', water: 'water', toilet: 'toilet', kitchen: 'kitchen' };

/** First card of an announcement (AI card or the keyword fallback), as words for reading aloud. */
export function headline(a: Announcement) {
  const c = a.cards?.[0];
  return { icon: cardIcon(c?.icon ?? a.icons[0] ?? 'info'), title: c?.title ?? a.text.split(/\s+/).slice(0, 4).join(' '), time: c?.time ?? a.times[0] ?? null, place: c?.place ?? a.place };
}
const spoken = (a: Announcement) => { const h = headline(a); return [h.title, h.time, h.place].filter(Boolean).join('. '); };

function LangSwitch() {
  const s = useAppState(), f = useF();
  return <div className="seg" role="group" aria-label="Language"><button aria-pressed={s.lang === 'en'} onClick={() => f.settings.setLang('en')}>EN</button><button aria-pressed={s.lang === 'id'} onClick={() => f.settings.setLang('id')}>ID</button></div>;
}
function Top({ title, sub }: { title: string; sub?: string }) {
  const t = useT();
  return <header className="ev-top"><button className="round-btn" aria-label={t.back} onClick={() => go('')}><Icon name="back" /></button><div className="grow"><b>{title}</b>{sub && <small>{sub}</small>}</div></header>;
}

export function Public({ tab }: { tab: string }) {
  return (
    <div className="app public">
      <div key={tab} style={{ display: 'contents' }}>
        {tab === 'help' ? <HelpPage /> : tab === 'map' ? <MapPage /> : tab === 'family' ? <Family /> : tab === 'ask' ? <AskKit /> : <Home />}
      </div>
    </div>
  );
}

function Home() {
  const t = useT(), s = useAppState(), f = useF();
  const news = f.views.news();
  const [latest, ...rest] = news;
  const h = latest && headline(latest);
  return (
    <>
      <header className="ev-head"><Kit mood={latest ? 'idle' : 'sleep'} size={74} /><div className="greet"><small>{s.shelter.name}</small><b>{greeting(t)}</b></div><LangSwitch /></header>
      <main className="content">
        {latest ? (
          <article className="news rise" aria-live="polite">
            <span className="eyebrow"><span className="live" />{t.now}</span>
            <div className="row"><span className="ico"><Icon name={h!.icon} size={46} sw={1.8} /></span>
              <div className="grow"><h3>{h!.title}</h3><div className="meta">{h!.time && <span><Icon name="clock" size={20} />{h!.time}</span>}{h!.place && <span><Icon name="pin" size={20} />{h!.place}</span>}</div></div></div>
            <p className="said">“{latest.text}”</p>
            <SayButton id={latest.id} text={spoken(latest)} />
          </article>
        ) : <div className="news rise"><span className="eyebrow">{t.now}</span><p className="h" style={{ fontSize: 26 }}>{t.noNews}</p></div>}
        {rest.length > 0 && <div className="news-more">{rest.slice(0, 2).map(a => { const x = headline(a); return (
          <div key={a.id} className="row-card"><span className="sq honey" style={{ width: 44, height: 44, borderRadius: 14, display: 'grid', placeItems: 'center', background: 'var(--honey)', color: 'var(--honey-acc)' }}><Icon name={x.icon} /></span>
            <span className="grow"><b>{x.title}</b> <span>{[x.time, x.place].filter(Boolean).join(' · ')}</span></span></div>); })}</div>}
        <BigBtn i={1} tone="sky" icon="map" title={t.map} sub={t.mapSub} onClick={() => go('map')} />
        <BigBtn i={2} tone="rose" icon="help" title={t.needHelp} sub={t.needHelpSub} onClick={() => go('help')} />
        <BigBtn i={3} tone="sage" icon="family" title={t.myFamily} sub={t.myFamilySub} onClick={() => go('family')} />
        <BigBtn i={4} tone="lav" icon="chat" title={t.askKit} sub={t.askKitSub} onClick={() => go('ask')} />
      </main>
    </>
  );
}
function BigBtn({ tone, icon, title, sub, onClick, i }: { tone: string; icon: string; title: string; sub: string; onClick: () => void; i: number }) {
  return <button className={`big-btn b-${tone} rise`} style={{ '--d': i } as CSSProperties} onClick={onClick}><span className="disc"><Icon name={icon} size={34} /></span><b>{title}<small>{sub}</small></b><span className="go"><Icon name="chevron" /></span></button>;
}

function HelpPage() {
  const f = useF(), t = useT();
  const [spot, setSpot] = useState('');
  const [sent, setSent] = useState('');
  const [writing, setWriting] = useState(false);
  const [msg, setMsg] = useState('');
  const kinds: [string, string, string][] = [['medicine', 'medicine', 'sage'], ['sick', 'sick', 'rose'], ['moving', 'mobility', 'sky'], ['write', 'chat', 'lav']];
  const label = (k: string) => ({ medicine: t.helpMedicine, sick: t.helpSick, moving: t.helpMoving, write: t.helpWrite })[k]!;
  const send = (k: string, text?: string) => { f.settings.help(k, spot.trim() || '?', text); setSent(k); setWriting(false); setMsg(''); };
  return (
    <>
      <Top title={t.needHelp} sub={t.helpTap} />
      <main className="content">
        <label className="spot"><span className="disc"><Icon name="bed" /></span><span className="grow"><small>{t.mySpot}</small><input value={spot} placeholder={t.spotHint} onChange={e => setSpot(e.target.value)} /></span></label>
        <div className="tiles">{kinds.map(([k, icon, tone], i) => (
          <button key={k} className={`tile b-${tone} rise ${sent === k ? 'sel' : ''}`} style={{ '--d': i } as CSSProperties} onClick={() => (k === 'write' ? setWriting(true) : send(k))}>
            {sent === k && <span className="sent"><Icon name="check" size={15} sw={3} />{t.sent}</span>}
            <span className="disc"><Icon name={icon} size={30} /></span><b>{label(k)}</b></button>
        ))}</div>
        {writing && <div className="ask-box rise"><textarea rows={3} value={msg} placeholder={t.writeHint} aria-label={t.helpWrite} onChange={e => setMsg(e.target.value)} />
          <button className="btn primary" disabled={!msg.trim()} onClick={() => send('write', msg.trim())}><Icon name="send" />{t.send}</button></div>}
        {sent && <div className="toast rise" role="status"><Kit mood="idle" size={52} /><span>{fmt(t.volunteerComing, { spot: spot.trim() || t.mySpot })}</span></div>}
      </main>
    </>
  );
}

function MapPage() {
  const s = useAppState(), f = useF(), t = useT();
  const sh = pack.shelters.find(x => x.id === s.shelter.id) ?? pack.shelters[0];
  const higher = nearestHigher(pack, sh.lat, sh.lng, f.rules.currentEvent, f.rules.higherMinGainM);
  const hs = higher && pack.shelters.find(x => x.id === higher.id);
  const inside = pack.places.map(p => ({ ...p, m: Math.round(distanceM(sh.lat, sh.lng, p.lat, p.lng)) }));
  const say = higher ? `${t.higherPlace}. ${fmt(t.higherAway, { name: higher.name, m: higher.distanceM, dir: higher.direction, gain: higher.handGain })}` : t.higherPlace;
  return (
    <>
      <Top title={t.map} />
      <ShelterMap you={sh} markers={hs ? [{ id: hs.id, lat: hs.lat, lng: hs.lng, icon: 'higher', color: '#2F7A52', label: t.higherPlace }] : []}
        chips={<><span className="chip"><i style={{ background: '#B9D1E8' }} />{t.floodRisk}</span><span className="chip"><i style={{ background: '#2F7A52' }} />{t.higherPlace}</span><span className="chip"><i style={{ background: '#1C2F4A' }} />{t.you}</span></>} />
      <div className="sheet">
        <span className="grab" />
        {higher && <div className="place"><span className="avatar" style={{ background: 'var(--sage)', color: 'var(--sage-acc)' }}><Icon name="higher" size={28} /></span>
          <div className="tx"><b className="h" style={{ fontSize: 24, color: 'var(--navy)' }}>{t.higherPlace}</b><span style={{ fontSize: 17 }}>{fmt(t.higherAway, { name: higher.name, m: higher.distanceM, dir: higher.direction, gain: higher.handGain })}</span></div></div>}
        <SayButton id="map-higher" text={say} className="b-sky" />
        <div className="inside">{inside.map(p => (
          <div key={p.name}><span className="disc" style={{ background: p.kind === 'medical' ? 'var(--rose)' : 'var(--sky)', color: p.kind === 'medical' ? 'var(--rose-acc)' : 'var(--sky-acc)' }}><Icon name={PLACE_ICON[p.kind] ?? 'pin'} /></span>{t[PLACE_WORD[p.kind] ?? 'doctor']}<span className="muted" style={{ fontSize: 13 }}>{p.m} m</span></div>
        ))}</div>
        {s.open && s.open.places.length > 0 && <div className="card"><h3><Icon name="clock" />{t.openNearby}</h3>
          {s.open.places.map(p => <div key={p.name} className="place"><div className="tx"><b>{p.name}</b><span>{[p.openNow ? t.openNow : null, p.hours, p.distanceKm != null ? fmt(t.kmAway, { km: p.distanceKm }) : null].filter(Boolean).join(' · ')}</span></div></div>)}
          <Badge source="maps" /></div>}
        <p className="note"><Icon name="info" size={16} />{fmt(t.hazardNote, { event: f.rules.currentEvent })}</p>
      </div>
    </>
  );
}

function Family() {
  const t = useT(), f = useF(), s = useAppState();
  const [q, setQ] = useState('');
  const [res, setRes] = useState<Awaited<ReturnType<typeof f.family.search>> | null>(null);
  return (
    <>
      <Top title={t.myFamily} sub={t.everyShelter} />
      <main className="content">
        <form className="search big" onSubmit={async e => { e.preventDefault(); if (q.trim()) setRes(await f.family.search(q)); }}>
          <Icon name="search" size={28} /><input value={q} placeholder={t.search} aria-label={t.search} onChange={e => setQ(e.target.value)} />
          <button className="btn primary small" type="submit">{t.search}</button>
        </form>
        {res && res.hits.length === 0 && <p className="h" style={{ fontSize: 22 }}>{t.notFound}</p>}
        {res?.hits.map((h, i) => (
          <div key={i} className="family-hit rise" style={{ '--d': i } as CSSProperties}><span className="avatar" style={{ width: 66, height: 66, fontSize: 28 }}>{h.name.replace(/^(Pak|Ibu|Bu|Bapak|Nenek|Kakek)\s+/i, '').slice(0, 1)}</span>
            <div className="grow"><b>{h.name}</b><span>{h.ageBand} · {h.shelterId}</span><div style={{ marginTop: 8 }}><span className={`pill ${h.status === 'safe' ? 'safe' : 'warn'}`}><Icon name="check" size={16} />{h.status === 'safe' ? t.safe : h.status}</span></div></div></div>
        ))}
        {(!res || !res.cloud) && s.signal !== 'back' && <p className="note"><Icon name="device" size={16} />{t.offlineFamily}</p>}
      </main>
    </>
  );
}

function AskKit() {
  const t = useT(), f = useF();
  const [q, setQ] = useState('');
  const [busy, setBusy] = useState(false);
  const [r, setR] = useState<Awaited<ReturnType<typeof f.hybrid.askKit>> | null>(null);
  const ask = async () => { setBusy(true); setR(null); setR(await f.hybrid.askKit(q)); setBusy(false); };
  return (
    <>
      <Top title={t.askKit} sub={t.askPrivate} />
      <main className="content">
        <div className="ask-box">
          <textarea rows={2} value={q} placeholder={t.askKitHint} aria-label={t.askKit} onChange={e => setQ(e.target.value)} />
          <div className="tools"><MicButton onText={setQ} /><button className="btn primary" style={{ flex: 1 }} disabled={!q.trim() || busy} onClick={ask}><Icon name="send" />{t.ask}</button></div>
        </div>
        {busy && <div className="empty"><Kit mood="think" size={110} /></div>}
        {r && (r.answer ? (
          <div className="answer rise" aria-live="polite">
            <div className="place"><Kit mood={r.source === 'cloud' ? 'cloud' : 'device'} size={56} /><Badge source={r.source} /></div>
            <p>{r.answer}</p>
            {r.card && <div className="from"><Icon name={headline(r.card).icon} />{t.fromNotice}: “{r.card.text}”</div>}
            <SayButton id="ask" text={r.answer} />
          </div>
        ) : (
          <div className="answer rise" aria-live="polite"><div className="place"><Kit mood="idle" size={56} /></div><p>{t.askVolunteer}</p>
            <SayButton id="ask" text={t.askVolunteer} />
            <button className="btn danger" onClick={() => go('help')}><Icon name="help" />{t.needHelp}</button></div>
        ))}
      </main>
    </>
  );
}

// ---------- Big Screen: the hall TV, readable from 10 m. Rotates through shown notices. ----------
const SLIDE_MS = 9000;
let tick = 0;
const tickListeners = new Set<() => void>();
setInterval(() => { tick++; tickListeners.forEach(l => l()); }, SLIDE_MS);
const useTick = () => useSyncExternalStore(l => { tickListeners.add(l); return () => { tickListeners.delete(l); }; }, () => tick);

export function BigScreen() {
  const f = useF(), s = useAppState(), n = useTick(), t = useT();
  const news = f.views.news(), sch = f.views.schedule();
  const a = news.length ? news[n % news.length] : null;
  const h = a && headline(a);
  const next = Object.entries(sch.counts)[0];
  const sh = pack.shelters.find(x => x.id === s.shelter.id) ?? pack.shelters[0];
  const higher = nearestHigher(pack, sh.lat, sh.lng, f.rules.currentEvent, f.rules.higherMinGainM);
  const date = new Date().toLocaleDateString(s.lang === 'id' ? 'id-ID' : 'en-GB', { weekday: 'long', day: 'numeric', month: 'long' });
  return (
    <div className="tv">
      <div className="tv-main">
        <div className="tv-brand"><Kit mood="idle" size={84} /><div><b>{s.shelter.name}</b><span>{date}</span></div></div>
        {h ? <div className="tv-news" key={`${a!.id}-${n}`}><span className="eyebrow"><span className="live" />{t.now}</span><h3>{h.title}</h3>
          <div className="meta">{h.time && <span><Icon name="clock" size={40} />{h.time}</span>}{h.place && <span><Icon name="pin" size={40} />{h.place}</span>}</div><p className="said">“{a!.text}”</p></div>
          : <div className="tv-news"><Kit mood="sleep" size={220} /><h3 style={{ fontSize: 'clamp(40px,5vw,80px)' }}>{t.noNews}</h3></div>}
      </div>
      <div className="tv-side">
        {next && <div className="tv-card" style={{ background: 'var(--sage)', color: 'var(--sage-ink)' }}><small>{t.next}</small><b>{t.medicine} {next[0]}</b><span>{t.stayAtBed}</span></div>}
        {higher && <div className="tv-card" style={{ background: 'var(--sky)', color: 'var(--sky-ink)' }}><small>{t.ifWaterRises}</small><b>{fmt(t.goTo, { name: higher.name })}</b><span>{higher.distanceM} m {higher.direction} · +{higher.handGain} m</span></div>}
        <div className="tv-qr"><QR text={`${location.href.split('#')[0]}#/public`} size={140} label={t.scanForNews} /><div><b>{t.scanForNews}</b><span>{t.scanSub}</span></div></div>
      </div>
      {news.length > 1 && <div className="tv-progress" key={`p${n}`} style={{ '--ms': `${SLIDE_MS}ms` } as CSSProperties}><i /></div>}
    </div>
  );
}

