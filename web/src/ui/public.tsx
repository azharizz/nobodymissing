// Evacuee app (elderly and deaf first: huge type, read aloud, dark text on soft colour) + the Big Screen for the hall TV.
import { useState, useSyncExternalStore, type CSSProperties } from 'react';
import { regionPack as pack } from '../features/regionData';
import { distanceM, nearestHigher } from '../domain/regionPack';
import type { Announcement } from '../features/state';
import { fmt } from './i18n';
import { GoNow, Way, PickupPage, CheckinCard } from './firstHours';
import { Badge, cardIcon, greeting, handOff, Icon, takeHandOff, Kit, MicButton, navigate, QR, QrScanner, SayButton, ShelterMap, useAppState, useF, useT } from './common';

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
        {tab === 'go' ? <GoNow /> : tab === 'way' ? <Way /> : tab === 'pickup' ? <PickupPage /> : tab === 'checkin' ? <CheckinCard /> : tab === 'help' ? <HelpPage /> : tab === 'map' ? <MapPage /> : tab === 'family' ? <Family /> : tab === 'ask' ? <AskKit /> : <Home />}
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
        {s.warning && s.warning.level !== 'advice' && <button className="alert-banner rise" onClick={() => go('go')}><Kit mood="alert" size={56} /><span className="grow"><b>{(t as Record<string, string>)[`hazard_${s.warning.hazard}`]} · {(t as Record<string, string>)[`level_${s.warning.level}`]}</b>{s.warning.plain}</span><Icon name="chevron" /></button>}
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
        <BigBtn i={1} tone="rose" icon="warn" title={t.goSafety} sub={t.goSafetySub} onClick={() => go('go')} />
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
  const [sent, setSent] = useState<{ kind: string; urgent: boolean } | null>(null);
  const [words, setWords] = useState(takeHandOff);
  const [busy, setBusy] = useState(false);
  const [tri, setTri] = useState<Awaited<ReturnType<typeof f.hybrid.triage>> | null>(null);
  const kinds: [string, string, string][] = [['medicine', 'medicine', 'sage'], ['sick', 'sick', 'rose'], ['moving', 'mobility', 'sky'], ['baby', 'baby', 'lav']];
  const send = (kind: string, text?: string, more: Parameters<typeof f.settings.help>[3] = {}) => { f.settings.help(kind, spot.trim() || '?', text, more); setSent({ kind, urgent: !!more.urgent }); setTri(null); setWords(''); };
  const understand = async () => { setBusy(true); setTri(await f.hybrid.triage(words)); setBusy(false); };
  return (
    <>
      <Top title={t.needHelp} sub={t.helpTap} />
      <main className="content">
        <label className="spot"><span className="disc"><Icon name="bed" /></span><span className="grow"><small>{t.mySpot}</small><input value={spot} placeholder={t.spotHint} onChange={e => setSpot(e.target.value)} /></span></label>
        <div className="ask-box">
          <div className="place"><Kit mood={busy ? 'think' : 'idle'} size={52} /><b className="h" style={{ fontSize: 22, color: 'var(--navy)' }}>{t.tellKit}</b></div>
          <textarea rows={2} value={words} placeholder={t.tellKitHint} aria-label={t.tellKit} onChange={e => { setWords(e.target.value); setTri(null); }} />
          <div className="tools"><MicButton onText={w => { setWords(w); setTri(null); }} /><button className="btn primary" style={{ flex: 1 }} disabled={!words.trim() || busy} onClick={understand}><Icon name="sparkle" />{t.ask}</button></div>
        </div>
        {tri && <div className={`answer rise ${tri.urgent ? 'urgent' : ''}`}>
          <span className="eyebrow">{t.kitUnderstood}</span>
          <div className="pills"><span className="pill" style={{ background: '#fff' }}>{(t as Record<string, string>)[`kind_${tri.kind}`] ?? tri.kind}</span>{tri.urgent && <span className="pill urgent"><Icon name="alert" size={14} />{t.urgent}</span>}</div>
          <p>{tri.summary}</p>
          <span className={`badge ${tri.by === 'device' ? 'local' : 'kw'}`}><Icon name={tri.by === 'device' ? 'device' : 'keywords'} size={15} />{tri.by === 'device' ? t.understoodOnPhone : t.keywordsOnly}</span>
          <button className="btn primary" onClick={() => send(tri.kind, words.trim(), { summary: tri.summary, urgent: tri.urgent, by: tri.by })}><Icon name="send" />{t.sendToStaff}</button>
        </div>}
        <span className="preview-label">{t.orTapOne}</span>
        <div className="tiles">{kinds.map(([k, icon, tone], i) => (
          <button key={k} className={`tile b-${tone} rise ${sent?.kind === k ? 'sel' : ''}`} style={{ '--d': i } as CSSProperties} onClick={() => send(k)}>
            {sent?.kind === k && <span className="sent"><Icon name="check" size={15} sw={3} />{t.sent}</span>}
            <span className="disc"><Icon name={icon} size={30} /></span><b>{(t as Record<string, string>)[`kind_${k}`]}</b></button>
        ))}</div>
        {sent && <div className={`toast rise ${sent.urgent ? 'urgent' : ''}`} role="status"><Kit mood={sent.urgent ? 'alert' : 'idle'} size={52} /><span>{sent.urgent ? t.urgentNow : fmt(t.volunteerComing, { spot: spot.trim() || t.mySpot })}</span></div>}
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
  const [q, setQ] = useState(takeHandOff);
  const [busy, setBusy] = useState(false);
  const [res, setRes] = useState<Awaited<ReturnType<typeof f.family.smart>> | null>(null);
  const go = async () => { if (!q.trim()) return; setBusy(true); setRes(await f.family.smart(q)); setBusy(false); };
  return (
    <>
      <Top title={t.myFamily} sub={t.everyShelter} />
      <main className="content">
        <div className="ask-box">
          <textarea rows={2} value={q} placeholder={t.lookForHint} aria-label={t.lookFor} onChange={e => setQ(e.target.value)} />
          <div className="tools"><MicButton onText={setQ} /><button className="btn primary" style={{ flex: 1 }} disabled={!q.trim() || busy} onClick={go}><Icon name="search" className={busy ? 'spin' : ''} />{t.search}</button></div>
        </div>
        {res && <div className="card rise" style={{ gap: 8 }}>
          <p><b className="h" style={{ fontSize: 18 }}>{t.lookingFor}: {res.name}</b>{res.age != null && <span className="muted"> · {fmt(t.aged, { n: res.age })}</span>}{res.place && <span className="muted"> · {fmt(t.fromPlace, { p: res.place })}</span>}</p>
          {res.parsedBy === 'device' ? <span className="badge local"><Icon name="device" size={15} />{t.understoodOnPhone}</span> : <span className="badge kw"><Icon name="keywords" size={15} />{t.keywordsOnly}</span>}
          {res.variants.length > 0 && <p className="muted">{t.spellings}: {res.variants.join(', ')} <span className={`badge ${res.variantsBy === 'cloud' ? 'cloud' : 'kw'}`} style={{ marginTop: 6 }}><Icon name={res.variantsBy === 'cloud' ? 'cloud' : 'device'} size={15} />{res.variantsBy === 'cloud' ? t.spellingsCloud : t.spellingsOffline}</span></p>}
        </div>}
        {res && res.hits.length === 0 && <p className="h" style={{ fontSize: 22 }}>{t.notFound}</p>}
        {res?.hits.map((h, i) => (
          <div key={i} className="family-hit rise" style={{ '--d': i } as CSSProperties}><span className="avatar" style={{ width: 66, height: 66, fontSize: 28 }}>{h.name.replace(/^(Pak|Ibu|Bu|Bapak|Nenek|Kakek)\s+/i, '').slice(0, 1)}</span>
            <div className="grow"><b>{h.name}</b><span>{h.ageBand} · {h.shelterId}</span>
              <div className="pills" style={{ marginTop: 8 }}><span className={`pill ${h.status === 'safe' ? 'safe' : 'warn'}`}><Icon name="check" size={16} />{h.status === 'safe' ? t.safe : h.status}</span>
                <span className={`pill ${h.match === 'exact' ? '' : 'warn'}`}>{h.match === 'exact' ? t.matchExact : t.matchSpelling}</span></div></div></div>
        ))}
        {res && res.hits.length > 0 && <p className="note"><Icon name="info" size={16} />{t.confirmWithStaff}</p>}
        {(!res || !res.cloud) && s.signal !== 'back' && <p className="note"><Icon name="device" size={16} />{t.offlineFamily}</p>}
        <FamilyGroup />
      </main>
    </>
  );
}

/** Family group by QR: show mine, scan theirs. Works offline face to face; syncs online when apart. */
function FamilyGroup() {
  const t = useT(), f = useF(), s = useAppState();
  const g = s.familyGroup;
  const [name, setName] = useState(g?.me.name ?? '');
  const [show, setShow] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [paste, setPaste] = useState('');
  const [msg, setMsg] = useState('');
  const got = (text: string) => { const m = f.familyGroup.scan(text); setMsg(m ? `${fmt(t.linked, { name: m.map(x => x.name).join(', ') })} ${t.scanBack}` : t.badQr); if (m) { setScanning(false); setPaste(''); setShow(true); } return !!m; };
  const ago = (at: number) => { const m = Math.round((Date.now() - at) / 60000); return m < 1 ? t.justNow : m < 60 ? fmt(t.minAgo, { n: m }) : fmt(t.hAgo, { n: Math.round(m / 60) }); };
  const row = (m: { id: string; name: string; shelter: string; status: string; at: number }, me = false) => (
    <div key={m.id} className="family-hit" style={{ padding: 14 }}><span className="avatar" style={me ? { background: 'var(--honey)', color: 'var(--honey-ink)' } : undefined}>{m.name.slice(0, 1).toUpperCase()}</span>
      <div className="grow"><b style={{ fontSize: 20 }}>{me ? `${m.name} · ${t.me}` : m.name}</b><span style={{ fontSize: 15 }}>{m.shelter} · {fmt(t.updatedAgo, { when: ago(m.at) })}{!me && g?.via[m.id] ? ` · ${g.via[m.id] === 'qr' ? t.viaQr : t.viaOnline}` : ''}</span></div>
      <span className={`pill ${m.status === 'safe' ? 'safe' : 'urgent'}`}>{(t as Record<string, string>)[`status_${m.status}`]}</span></div>
  );
  return (
    <section className="card" style={{ background: 'var(--sage)', color: 'var(--sage-ink)', gap: 12 }}>
      <h3><Icon name="family" />{t.familyGroup}</h3>
      <p style={{ fontSize: 16 }}>{t.familyGroupWhy}</p>
      <label className="field" style={{ color: 'var(--sage-ink)' }}>{t.yourName}<input value={name} onChange={e => setName(e.target.value)} /></label>
      <div className="seg" role="group" aria-label={t.myStatus}>{(['safe', 'need_help'] as const).map(st => (
        <button key={st} aria-pressed={(g?.me.status ?? 'safe') === st} disabled={!name.trim()} onClick={() => f.familyGroup.setMe(name, st)}>{(t as Record<string, string>)[`status_${st}`]}</button>))}</div>
      <div className="cta fam" style={{ gridTemplateColumns: '1fr 1fr' }}>
        <button className="btn" disabled={!name.trim()} onClick={() => { if (!g || g.me.name !== name.trim()) f.familyGroup.setMe(name, g?.me.status ?? 'safe'); setShow(!show); setScanning(false); }}><Icon name="qr" />{t.showMyQr}</button>
        <button className="btn primary" disabled={!name.trim()} onClick={() => { if (!g) f.familyGroup.setMe(name, 'safe'); setScanning(!scanning); setShow(false); }}><Icon name="camera" />{t.scanFamilyQr}</button>
      </div>
      {show && g && <div className="qr-show rise"><QR text={f.familyGroup.myQr()!} size={260} label={t.showMyQr} /><span className="muted">{g.me.name} · {g.me.shelter}</span><span className="muted" style={{ textAlign: 'center' }}>{t.groupInQr}</span></div>}
      {scanning && <QrScanner onText={got} />}
      {scanning && <div className="tools"><input className="paste" value={paste} placeholder={t.pasteCode} aria-label={t.pasteCode} onChange={e => setPaste(e.target.value)} /><button className="btn small" disabled={!paste.trim()} onClick={() => got(paste)}>{t.import}</button></div>}
      {msg && <p className="note" role="status" style={{ color: 'var(--sage-ink)' }}><Icon name="info" size={16} />{msg}</p>}
      {g && row(g.me, true)}
      {g?.members.map(m => row(m))}
      {g && !g.members.length && <p style={{ fontSize: 16 }}>{t.noFamilyYet}</p>}
      <p className="note" style={{ color: 'var(--sage-ink)' }}><Icon name="device" size={16} />{t.familyOfflineNote}</p>
    </section>
  );
}

function AskKit() {
  const t = useT(), f = useF();
  const [q, setQ] = useState('');
  const [busy, setBusy] = useState(false);
  const [r, setR] = useState<Awaited<ReturnType<typeof f.hybrid.askKit>> | null>(null);
  const ask = async (text = q) => { if (!text.trim()) return; setQ(text); setBusy(true); setR(null); setR(await f.hybrid.askKit(text)); setBusy(false); };
  // The model only picks the intent; these words and buttons are the app's own (rules in code).
  const dest = r?.intent === 'where_to_go' ? f.first.destination() : null;
  const reply = !r ? '' : r.intent === 'notice' ? r.answer! : r.intent === 'where_to_go'
    ? (dest ? fmt(t.goAnswer, { name: dest.name, m: dest.distanceM, dir: dest.direction, gain: dest.handGain, where: dest.gps ? t.myLocation : t.theShelter }) : t.noHigher)
    : r.intent === 'greeting' ? t.kitHi : r.intent === 'help' ? (r.urgent ? t.kitUrgent : t.kitToHelp) : r.intent === 'pickup' ? t.kitToPickup : r.intent === 'family' ? t.kitToFamily : t.kitUnknown;
  const main = !r ? null : r.intent === 'where_to_go' && dest ? { to: 'way', icon: 'nav', label: t.showWay, cls: 'primary' }
    : r.intent === 'help' ? { to: 'help', icon: 'help', label: t.needHelp, cls: 'danger' } : r.intent === 'pickup' ? { to: 'pickup', icon: 'mobility', label: t.needPickup, cls: 'danger' }
    : r.intent === 'family' ? { to: 'family', icon: 'family', label: t.findFamily, cls: 'primary' } : null;
  const chips = [{ q: t.whereGo, icon: 'higher' }, { to: 'pickup', label: t.needPickup, icon: 'mobility' }, { to: 'help', label: t.needHelp, icon: 'help' }, { to: 'family', label: t.findFamily, icon: 'family' }, { to: '', label: t.todayNotices, icon: 'announce' }];
  const by = r?.source === 'cloud' ? 'cloud' : r?.source === 'device' ? 'device' : 'rules';
  return (
    <>
      <Top title={t.askKit} sub={t.askPrivate} />
      <main className="content">
        {!r && <div className="hello rise"><Kit mood={busy ? 'think' : 'idle'} size={busy ? 130 : 120} /><p className="bubble">{busy ? t.kitThinking : t.askKitHello}</p></div>}
        <div className="ask-box">
          <textarea rows={2} value={q} placeholder={t.askKitHint} aria-label={t.askKit} onChange={e => setQ(e.target.value)} onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void ask(); } }} />
          <div className="tools"><MicButton onText={x => void ask(x)} /><button className="btn primary" style={{ flex: 1 }} disabled={!q.trim() || busy} onClick={() => void ask()}><Icon name="send" className={busy ? 'spin' : ''} />{t.ask}</button></div>
        </div>
        {r && (
          <div className="answer rise" aria-live="polite">
            <div className="place"><Kit mood={by === 'cloud' ? 'cloud' : 'device'} size={56} />
              {by === 'rules' && r.intent === 'where_to_go' ? <span className="badge local"><Icon name="device" size={15} />{t.fromFloodMap}</span>
                : by === 'rules' ? <span className="badge kw"><Icon name="keywords" size={15} />{t.plainByRules}</span>
                : r.intent === 'notice' ? <Badge source={r.source as 'device' | 'cloud'} /> : <span className={`badge ${by === 'cloud' ? 'cloud' : 'local'}`}><Icon name={by} size={15} />{t.kitUnderstood}</span>}</div>
            <p>{reply}</p>
            {r.card && <div className="from"><Icon name={headline(r.card).icon} />{t.fromNotice}: “{r.card.text}”</div>}
            <SayButton id="ask" text={reply} />
            {main && <button className={`btn ${main.cls}`} onClick={() => { handOff(q); go(main.to); }}><Icon name={main.icon} />{main.label}</button>}
          </div>
        )}
        {!busy && <div className="kit-chips" role="group" aria-label={t.askKit}>
          {chips.filter(c => !(r && main && c.to === main.to)).map(c => <button key={c.icon} className="chip" onClick={() => c.q ? void ask(c.q) : go(c.to!)}><Icon name={c.icon} size={18} />{c.q ?? c.label}</button>)}
        </div>}
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

