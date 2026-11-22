// Staff app: a calm command centre. Now · Map · Camera (centre) · Announce · People. Care card, round, review and settings are sub-screens.
import { useState, type CSSProperties } from 'react';
import { drugList as drugs, regionPack as pack } from '../features/regionData';
import type { RouteResult } from '../features/inferenceRouter';
import type { ImageInput } from '../ports';
import type { Person } from '../features/state';
import { fmt } from './i18n';
import { Badge, canvasImage, Empty, greeting, Icon, Kit, MicButton, navigate, PhotoPick, QR, QrScanner, RouteNotice, ShelterMap, useAppState, useSpeaker, useF, useT, type T } from './common';
import { headline } from './public';
import { decodeCheckinQr, decodePickupQr } from '../domain/firstHours';
import evals from '../../../shared/eval/results.json';

const go = (p: string) => navigate(`#/staff/${p}`);
const nice = (s: string) => s.replace(/_/g, ' ');
const initial = (n: string) => n.replace(/^(Pak|Ibu|Bu|Bapak|Nenek|Kakek|M\.)\s+/i, '').slice(0, 1).toUpperCase();
const TONES = ['sky', 'rose', 'sage', 'lav'];

/** The last photo and AI result, handed from the camera to the screen that checks it (memory only, never stored). */
let last: { url: string; img: ImageInput; r: RouteResult; kind: 'list' | 'medicine'; personId?: string } | null = null;

export function Staff({ path }: { path: string[] }) {
  const s = useAppState();
  const [tab, arg] = path;
  const screen = tab || 'now';
  if (screen === 'camera') return <Camera />;
  return (
    <div className="app staff">
      {s.scoutAlert && <ScoutAlert />}
      <div key={screen + (arg ?? '')} style={{ display: 'contents' }}>
        {screen === 'checkin' ? <CheckIn /> : screen === 'map' ? <FieldMap /> : screen === 'announce' ? <Announce /> : screen === 'people' ? <People /> : screen === 'person' ? <CareCard id={arg} />
          : screen === 'review' ? <Review /> : screen === 'round' ? <Round /> : screen === 'settings' ? <Settings /> : screen === 'print' ? <PrintLists /> : screen === 'kit' ? <KitLog /> : <Now />}
      </div>
      <TabBar on={screen} />
    </div>
  );
}

function TabBar({ on }: { on: string }) {
  const t = useT();
  const tab = (k: string, icon: string, label: string) => <button aria-current={on === k || (k === 'people' && on === 'person')} onClick={() => go(k)}><Icon name={icon} />{label}</button>;
  return (
    <nav className="tabbar" aria-label="Staff">
      {tab('now', 'home', t.tabNow)}{tab('map', 'map', t.map)}
      <button className="cam" aria-label={t.tabCamera} onClick={() => go('camera')}><Icon name="camera" size={30} /></button>
      {tab('checkin', 'door', t.checkIn)}{tab('people', 'people', t.people)}
    </nav>
  );
}

function Top({ title, back = 'now', children }: { title: string; back?: string; children?: React.ReactNode }) {
  const t = useT();
  return <header className="screen-top"><button className="round-btn" aria-label={t.back} onClick={() => go(back)}><Icon name="back" /></button><b>{title}</b>{children}</header>;
}

/** Hybrid made visible: how many answers ran on this phone vs the cloud today, and what (if anything) left the shelter. */
function KitCard() {
  const t = useT(), f = useF(); useAppState();
  const k = f.views.kit(), w = f.views.counts().waiting;
  const left = (Object.entries(k.sent) as [string, number][]).filter(([, n]) => n > 0);
  const personal = k.sent.photo + k.sent.name;
  return (
    <button className="kit-card rise" onClick={() => go('kit')} aria-label={t.whereKitRan}>
      <span className="kc-h"><b>{t.kitToday}</b><span className="muted">{fmt(t.nWaiting, { n: w })}</span></span>
      <span className="kc-n">
        <span className="dev"><b>{k.device}</b><span><Icon name="device" size={15} />{t.onPhoneBig}</span></span>
        <span className="cld"><b>{k.cloud}</b><span><Icon name="cloud" size={15} />{t.inCloudBig}</span></span>
      </span>
      <span className={`kc-left ${personal ? 'warn' : 'ok'}`}><Icon name="lock" size={15} />
        {left.length === 0 ? t.leftNone : personal === 0 ? `${t.leftNothing} · ${left.map(([k2, n]) => fmt((t as Record<string, string>)[`sent_${k2}`], { n })).join(' · ')}`
          : left.map(([k2, n]) => fmt((t as Record<string, string>)[`sent_${k2}`], { n })).join(' · ')}</span>
      <span className="kc-tap">{t.kitTap}<Icon name="chevron" size={16} /></span>
    </button>
  );
}

/** Measured, not claimed: device vs cloud vs keywords on held-out test sets (shared/eval/results.json). */
function EvalCard() {
  const t = useT();
  return (<>{evals.sets.map(set => (
    <div key={set.id} className="card eval-card">
      <h3><Icon name="check" />{t.measuredTitle}</h3>
      <b>{set.title}</b><span className="muted">{fmt(t.measuredNote, { n: set.n, date: set.date })} · {set.where}</span>
      <table><thead><tr><th>{t.colWhere}</th><th>{t.colEn}</th><th>{t.colId}</th><th>{t.colTime}</th></tr></thead>
        <tbody>{set.rows.map(r => <tr key={r.path} className={r.path}><td>{r.label}</td><td><b>{r.en}%</b></td><td><b>{r.id}%</b></td><td>{r.medianMs ? `${(r.medianMs / 1000).toFixed(1)} s` : '—'}</td></tr>)}</tbody></table>
      <p className="note">{set.decision}</p>
    </div>))}</>);
}

function KitLog() {
  const t = useT(), f = useF(); useAppState();
  const k = f.views.kit(), sends = (f.rules as unknown as { cloudSends: Record<string, string> }).cloudSends ?? {};
  const T = t as Record<string, string>;
  const chip = (l: (typeof k.log)[number]) => l.source === 'device' ? ['dev', t.ranOnPhone] : l.source === 'cloud' ? ['cld', t.ranInCloud]
    : l.status === 'queued' ? ['wait', t.ranWaiting] : l.status === 'ask_staff' ? ['wait', t.ranAsked] : ['off', t.ranBlocked];
  return (
    <>
      <Top title={t.whereKitRan} />
      <main className="content">
        <KitCard />
        {k.log.length === 0 && <Empty mood="idle">{t.noKitYet}</Empty>}
        {k.log.length > 0 && <EvalCard />}
        {k.log.map((l, i) => { const [cls, label] = chip(l); return (
          <div key={i} className={`kit-row ${cls}`}>
            <span className="kr-chip"><Icon name={cls === 'dev' ? 'device' : cls === 'cld' ? 'cloud' : cls === 'wait' ? 'clock' : 'lock'} size={15} />{label}</span>
            <span className="grow"><b>{T[`task_${l.task}`] ?? l.task}</b>
              <small>{T[`why_${l.why}`]}{l.source ? ` · ${(l.ms / 1000).toFixed(1)} s` : ''}</small>
              <small className="kr-sent">{l.source === 'cloud' ? fmt(t.sentWhat, { what: T[`what_${sends[l.task]}`] ?? '' }) : t.sentNothing}</small></span>
            <span className="muted">{new Date(l.at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
          </div>); })}
      </main>
    </>
  );
}

// ---------- Now ----------
function Now() {
  const t = useT(), s = useAppState(), f = useF();
  const [more, setMore] = useState(false);
  const sch = f.views.schedule(), reach = f.views.reach(), field = f.views.field();
  const name = (id: string) => s.people.find(p => p.id === id)?.nameAsWritten ?? id;
  const waiting = s.jobs.filter(j => j.state !== 'sent').length;
  const back = s.signal === 'back';
  const p1 = field.filter(r => r.tier === 'P1' && r.status === 'unchecked');
  const slot = Object.keys(sch.slots)[0];
  const given = slot ? sch.slots[slot].filter(e => s.round[f.medicine.key(slot, e.personId, e.generic)] === 'taken').length : 0;
  const toCheck = s.people.filter(p => !p.confirmedBy).length;
  const tasks: React.ReactNode[] = [
    ...s.help.filter(h => !h.done).sort((a, b) => Number(!!b.urgent) - Number(!!a.urgent)).map(h => <div key={h.id} className={`task ${h.urgent ? 'urgent' : ''}`}><span className={`sq ${h.urgent ? 'rose' : h.kind === 'write' ? 'lav' : 'rose'}`}><Icon name={h.urgent ? 'alert' : h.kind === 'write' ? 'chat' : 'help'} /></span>
      <span className="tx">{h.urgent && <span className="tagline">{t.urgent}</span>}<b>{fmt(t.asksFor, { spot: !h.spot.trim() || h.spot === '?' ? t.someone : h.spot, kind: (t as Record<string, string>)[`kind_${h.kind}`] ?? helpWord(t, h.kind) })}</b>
        {h.summary && <span>{h.summary}{h.by === 'device' ? ' · ' + t.readsOnPhone : ''}</span>}{h.text && h.text !== h.summary && <span>“{h.text}”</span>}</span>
      <button className="act" aria-label={t.confirm} onClick={() => f.settings.helpDone(h.id)}><Icon name="check" /></button></div>),
    ...sch.alerts.map(a => <button key={'a' + a.personId + a.generic} className="task urgent" onClick={() => go(`person/${a.personId}`)}><span className="sq"><Icon name="medicine" /></span>
      <span className="tx"><span className="tagline">{t.cantMiss}</span><b>{name(a.personId)} · {a.generic}</b><span>{fmt(t.onlyDaysLeft, { n: a.supplyDays ?? '?' })}</span></span><span className="act plain"><Icon name="chevron" /></span></button>),
    ...(s.batch.length ? [<button key="batch" className="task" onClick={() => f.roster.processBatch()}><span className="sq lav"><Icon name="clock" /></span>
      <span className="tx"><b>{fmt(t.photosWaiting, { n: s.batch.length })}</b><span>{t.processNow}</span></span><span className="act"><Icon name="sync" /></span></button>] : []),
    ...p1.map(r => <button key={'f' + r.id} className="task urgent" onClick={() => go(`map/${r.id}`)}><span className="sq"><Icon name="field" /></span>
      <span className="tx"><span className="tagline">{t.visitFirst}</span><b>{r.name}</b><span>{r.why.join(' · ')}</span></span><span className="act plain"><Icon name="chevron" /></span></button>),
    ...(slot ? [<button key="round" className="task" onClick={() => go('round')}><span className="sq honey"><Icon name="clock" /></span>
      <span className="tx"><b>{fmt(t.roundAt, { slot })}</b><span>{fmt(t.roundSub, { n: sch.slots[slot].length, given })}</span></span><span className="act plain"><Icon name="chevron" /></span></button>] : []),
    ...(toCheck ? [<button key="review" className="task" onClick={() => go('review')}><span className="sq sky"><Icon name="people" /></span>
      <span className="tx"><b>{t.checkList}</b><span>{fmt(t.toCheck, { n: toCheck })}</span></span><span className="act plain"><Icon name="chevron" /></span></button>] : []),
    ...(!back && s.lastSignalAt == null ? [<div key="tip" className="task"><span className="sq sage"><Icon name="power" /></span><span className="tx"><span>{t.airplaneTip}</span></span></div>] : []),
  ];
  const shown = more ? tasks : tasks.slice(0, 4);
  return (
    <>
      <header className="hero">
        <div className="hero-top"><span className="kit-tile"><Kit mood={back ? (waiting ? 'cloud' : 'idle') : 'device'} size={48} /></span>
          <div className="who"><small>{greeting(t)}</small><b>{s.shelter.name}</b></div>
          {f.env === 'demo' && <span className="pill warn" title={t.demoBadge}>DEMO</span>}
          <button className="icon-btn" aria-label={t.settings} onClick={() => go('settings')}><Icon name="settings" /></button></div>
        <button className={`signal ${back ? 'back' : ''}`} onClick={() => f.scout.check()}>
          <span className="lamp" /><span><b>{back ? t.signalOn : t.noSignalShort}</b> {back ? (waiting ? fmt(t.nWaiting, { n: waiting }) : t.allSent) : `${t.workingHere} · ${fmt(t.nWaiting, { n: waiting })}`}</span><span className="chev"><Icon name="sync" size={18} /></span></button>
        <div className="stats">
          <button className={`stat ${sch.alerts.length ? 'alarm' : ''}`} onClick={() => go('round')}><b>{sch.alerts.length}</b><span>{t.cantMiss}</span></button>
          <button className={`stat ${p1.length ? 'alarm' : ''}`} onClick={() => go('map')}><b>{p1.length}</b><span>{t.visitFirst}</span></button>
          <button className="stat" onClick={() => go('announce')}><b>{reach.remaining}</b><span>{t.toTell}</span></button>
        </div>
      </header>
      <main className="content" style={{ paddingTop: 18 }}>
        <KitCard />
        <div className="list-h"><b>{t.needsYouNow}</b><button className="chip" onClick={() => go('announce')}><Icon name="announce" size={16} />{t.announce}</button></div>
        {shown.map((x, i) => <div key={i} className="rise" style={{ '--d': i } as CSSProperties}>{x}</div>)}
        {tasks.length > 4 && <button className="btn link" onClick={() => setMore(!more)}>{more ? t.seeLess : fmt(t.seeMore, { n: tasks.length - 4 })}</button>}
        {tasks.length === 0 && (s.people.length ? <Empty mood="idle">{t.allClear}</Empty> : <Empty>{t.emptyShelter}</Empty>)}
      </main>
    </>
  );
}
const helpWord = (t: T, k: string) => ({ medicine: t.helpMedicine, sick: t.helpSick, moving: t.helpMoving, write: t.helpWrite } as Record<string, string>)[k] ?? nice(k);

// ---------- Camera: point at paper, press once. Modes like a phone camera. ----------
function Camera() {
  const t = useT(), s = useAppState(), f = useF();
  const q = new URLSearchParams(location.hash.split('?')[1] ?? '');
  const [mode, setMode] = useState<'list' | 'medicine' | 'report'>((q.get('mode') as 'list') || 'list');
  const [pid, setPid] = useState(q.get('person') ?? s.people.find(p => p.confirmedBy)?.id ?? '');
  const [video, setVideo] = useState<HTMLVideoElement | null>(null);
  const [noCam, setNoCam] = useState(false);
  const [busy, setBusy] = useState(false);
  const [note, setNote] = useState('');
  const [pos, setPos] = useState<{ lat: number; lng: number } | null>(null);
  const [r, setR] = useState<RouteResult | null>(null);

  // Live viewfinder via a ref callback (no useEffect): start on mount, stop the tracks on unmount.
  const attach = (el: HTMLVideoElement | null) => {
    if (!el) return;
    let stream: MediaStream | null = null, gone = false;
    navigator.mediaDevices?.getUserMedia({ video: { facingMode: 'environment', width: { ideal: 1920 } }, audio: false })
      .then(st => { if (gone) return st.getTracks().forEach(x => x.stop()); stream = st; el.srcObject = st; el.play().catch(() => {}); setVideo(el); })
      .catch(() => setNoCam(true));
    if (!navigator.mediaDevices) setNoCam(true);
    return () => { gone = true; stream?.getTracks().forEach(x => x.stop()); };
  };

  const run = async (img: ImageInput, url: string, cloud = false) => {
    setBusy(true);
    if (mode === 'list') { const res = await f.roster.scan(img, cloud); last = { url, img, r: res, kind: 'list' }; setBusy(false); go('review'); }
    else { const res = await f.careCard.read(pid, img, cloud); last = { url, img, r: res, kind: 'medicine', personId: pid }; setBusy(false); go(`person/${pid}`); }
  };
  const shoot = () => { if (!video?.videoWidth) return; const { image, url } = canvasImage(video, video.videoWidth, video.videoHeight); run(image, url); };
  const report = async (cloud = false) => { setBusy(true); const res = await f.field.report(note, pos, cloud); setBusy(false); setR(res); if (res.status === 'ok' || res.status === 'draft') { setNote(''); go('map'); } };
  const canShoot = mode !== 'medicine' || !!pid;

  return (
    <div className="app cam-app">
      {mode === 'report' ? (
        <div className="cam-report">
          <div className="cam-top" style={{ position: 'static' }}><button className="glass" aria-label={t.close} onClick={() => go('now')}><Icon name="x" /></button><span className="glass"><Icon name="device" size={16} />{t.readsOnPhone}</span></div>
          <textarea rows={5} value={note} placeholder={t.fieldHint} aria-label={t.report} onChange={e => setNote(e.target.value)} />
          <div className="tools"><MicButton onText={setNote} /><button className={`btn ${pos ? 'amber' : ''}`} onClick={() => navigator.geolocation?.getCurrentPosition(p => setPos({ lat: p.coords.latitude, lng: p.coords.longitude }))}><Icon name={pos ? 'check' : 'pin'} />GPS</button></div>
          <RouteNotice r={r} onCloud={() => report(true)} />
          <button className="btn amber full" disabled={!note.trim() || busy} onClick={() => report()}><Icon name="send" />{busy ? t.reading : t.report}</button>
        </div>
      ) : (
        <div className="viewfinder">
          <video ref={attach} playsInline muted />
          {!video && noCam && <div className="cam-empty"><Icon name="camera" size={40} /><p>{t.noCamera}</p></div>}
          {video && <span className="frame" aria-hidden />}
          <div className="cam-top"><button className="glass" aria-label={t.close} onClick={() => go('now')}><Icon name="x" /></button>
            {mode === 'medicine'
              ? <label className="glass">{t.forPerson}: <select value={pid} onChange={e => setPid(e.target.value)} aria-label={t.whoIsThisFor}><option value="">—</option>{s.people.map(p => <option key={p.id} value={p.id}>{p.nameAsWritten}</option>)}</select></label>
              : <span className="glass"><Icon name="device" size={16} />{t.readsOnPhone}</span>}</div>
          <div className="found">{busy ? <><Kit mood="think" size={26} />{t.reading}</> : !canShoot ? t.whoIsThisFor : <><span className="d" />{t.pointAtPage}</>}</div>
        </div>
      )}
      <div className="modes" role="group">
        {(['list', 'medicine', 'report'] as const).map(m => <button key={m} aria-pressed={mode === m} onClick={() => setMode(m)}>{m === 'list' ? t.modeList : m === 'medicine' ? t.modeMedicine : t.modeReport}</button>)}
      </div>
      {mode !== 'report' && <div className="shutter-row">
        <PhotoPick label={t.choosePhoto} className="thumbs" onPhoto={(img, url) => canShoot && run(img, url)}><Icon name="download" size={20} />{s.batch.length > 0 && <b>{s.batch.length}</b>}</PhotoPick>
        {video ? <button className={`shutter ${busy ? 'busy' : ''}`} aria-label={t.tabCamera} disabled={busy || !canShoot} onClick={shoot}><i /></button>
          : <PhotoPick label={t.tabCamera} className={`shutter ${busy ? 'busy' : ''}`} onPhoto={(img, url) => canShoot && run(img, url)}><i /></PhotoPick>}
        <button className="thumbs" aria-label={t.modeReport} onClick={() => setMode('report')}><Icon name="mic" size={20} /></button>
      </div>}
    </div>
  );
}

// ---------- Check the list: photo on top, unclear rows amber, nothing saved until a person confirms ----------
function Review() {
  const t = useT(), s = useAppState(), f = useF();
  const [open, setOpen] = useState<string | null>(null);
  const [r, setR] = useState<RouteResult | null>(last?.kind === 'list' ? last.r : null);
  const [busy, setBusy] = useState(false);
  const rows = s.people.filter(p => !p.confirmedBy);
  const clear = rows.filter(p => !p.needsReview);
  const src = rows[0]?.inferredBy;
  const retry = async () => { if (!last) return; setBusy(true); setR(await f.roster.scan(last.img, true)); setBusy(false); };
  return (
    <>
      <Top title={t.checkList} />
      <main className="content">
        {last?.kind === 'list' && <figure className={`photo ${busy ? 'scanning' : ''}`} style={{ margin: 0 }}><img src={last.url} alt={t.originalPhoto} /><span className="tagp">{t.originalPhoto}</span></figure>}
        {src === 'device' && <span className="badge local"><Icon name="device" size={15} />{t.readOnPhone}</span>}
        {src === 'cloud' && <span className="badge cloud"><Icon name="cloud" size={15} />{t.readByCloud}</span>}
        <RouteNotice r={rows.length ? null : r} onCloud={retry} />{/* rows read: the old "send to cloud?" question is answered */}
        {rows.map((p, i) => open === p.id ? <div key={p.id} className="card rise"><PersonForm p={p} onDone={() => setOpen(null)} /></div> : (
          <button key={p.id} className={`row-card rise ${p.needsReview ? 'check' : ''}`} style={{ '--d': i } as CSSProperties} onClick={() => setOpen(p.id)}>
            <b>{p.nameAsWritten}{p.age != null ? `, ${p.age}` : ''}{p.needsReview ? '?' : ''}</b>
            <span>{p.needsReview ? t.hardToRead : p.suggestedFlags.map(nice).join(' · ') || '—'}</span></button>))}
        {rows.length === 0 && <Empty mood="idle">{t.allChecked}</Empty>}
      </main>
      {rows.length > 0 && <div className="content" style={{ flex: 0, paddingTop: 0 }}><div className="cta">
        <button className="btn" onClick={() => setOpen((rows.find(p => p.needsReview) ?? rows[0]).id)}><Icon name="edit" />{t.fix}</button>
        <button className="btn primary" disabled={!clear.length} onClick={() => clear.forEach(p => f.roster.confirm(p.id, {}))}><Icon name="check" />{fmt(t.confirmN, { n: clear.length })}</button>
      </div></div>}
    </>
  );
}

const FLAGS = ['hard_of_hearing', 'deaf', 'needs_meds', 'wheelchair', 'cant_walk', 'lives_alone', 'foreign_language', 'infant', 'pregnant', 'elderly', 'dementia'];
function PersonForm({ p, onDone }: { p: Person; onDone: () => void }) {
  const t = useT(), f = useF();
  const [name, setName] = useState(p.nameAsWritten), [age, setAge] = useState(p.age?.toString() ?? ''), [flags, setFlags] = useState(p.suggestedFlags);
  return (
    <>
      <div className="field-row">
        <label className="field">Name<input value={name} onChange={e => setName(e.target.value)} /></label>
        <label className="field">Age<input inputMode="numeric" value={age} onChange={e => setAge(e.target.value)} /></label>
      </div>
      {p.notes && <p className="note"><Icon name="edit" size={16} />“{p.notes}”</p>}
      <div className="chips">{FLAGS.map(fl => (
        <label key={fl} className={`chip toggle ${flags.includes(fl) ? 'on' : ''}`}>
          <input type="checkbox" checked={flags.includes(fl)} onChange={e => setFlags(e.target.checked ? [...flags, fl] : flags.filter(x => x !== fl))} />{nice(fl)}
        </label>
      ))}</div>
      <button className="btn primary" onClick={() => { f.roster.confirm(p.id, { nameAsWritten: name.trim(), age: age ? Number(age) : null, flags }); onDone(); }}><Icon name="check" />{t.confirm}</button>
    </>
  );
}

// ---------- People ----------
function People() {
  const t = useT(), s = useAppState();
  const [q, setQ] = useState('');
  const list = s.people.filter(p => p.confirmedBy && p.nameAsWritten.toLowerCase().includes(q.toLowerCase()));
  const toCheck = s.people.filter(p => !p.confirmedBy).length;
  const hasMeds = new Set(s.cards.filter(c => c.meds.length).map(c => c.personId));
  return (
    <>
      <header className="screen-top"><b>{t.people}</b><span className="pill">{fmt(t.nPeople, { n: list.length })}</span></header>
      <main className="content">
        <label className="search"><Icon name="search" /><input placeholder={t.search} value={q} aria-label={t.search} onChange={e => setQ(e.target.value)} /></label>
        {toCheck > 0 && <button className="task" onClick={() => go('review')}><span className="sq sky"><Icon name="people" /></span><span className="tx"><b>{t.checkList}</b><span>{fmt(t.toCheck, { n: toCheck })}</span></span><span className="act plain"><Icon name="chevron" /></span></button>}
        {list.map((p, i) => (
          <button key={p.id} className="person rise" style={{ '--d': Math.min(i, 8) } as CSSProperties} onClick={() => go(`person/${p.id}`)}>
            <span className={`avatar sm`} style={{ background: `var(--${TONES[i % 4]})`, color: `var(--${TONES[i % 4]}-ink)` }}>{initial(p.nameAsWritten)}</span>
            <span className="grow"><b>{p.nameAsWritten}</b><small>{p.age ?? '?'} · {p.flags.map(nice).join(', ') || '—'}</small></span>
            {hasMeds.has(p.id) && <Icon name="medicine" />}<Icon name="chevron" />
          </button>
        ))}
        {s.people.length === 0 && <Empty>{t.emptyShelter}</Empty>}
      </main>
    </>
  );
}

// ---------- Care card: everything a medic needs at a glance + the two cloud helpers ----------
function CareCard({ id }: { id?: string }) {
  const t = useT(), s = useAppState(), f = useF();
  const p = s.people.find(x => x.id === id);
  const card = s.cards.find(c => c.personId === id);
  const [r, setR] = useState<RouteResult | null>(last?.kind === 'medicine' && last.personId === id ? last.r : null);
  if (!p || !id) return <><Top title={t.people} back="people" /><Empty>{t.notFound}</Empty></>;
  const meds = card?.meds ?? [];
  const pending = meds.map((m, i) => ({ m, i })).filter(x => !x.m.confirmedBy);
  const confirmed = meds.filter(m => m.confirmedBy);
  const days = (m: (typeof meds)[number]) => (m.qtyLeft != null && m.timesPerDay ? Math.floor(m.qtyLeft / m.timesPerDay) : null);
  const slots = f.rules.slotTimes as Record<string, string[]>;
  const read = [p.nameAsWritten, ...confirmed.map(m => `${m.generic} ${m.strength ?? ''}, ${m.doseAsWritten ?? ''}`)].join('. ');
  const photo = last?.kind === 'medicine' && last.personId === id ? last : null;
  const retry = async () => { if (photo) setR(await f.careCard.read(id, photo.img, true)); };
  return (
    <>
      <header className="profile">
        <div className="p-top"><button className="icon-btn" aria-label={t.back} onClick={() => go('people')}><Icon name="back" /></button><span className="grow" />
          <SpeakIcon id={`card-${id}`} text={read} /><button className="icon-btn" aria-label={t.print} onClick={() => print()}><Icon name="print" /></button></div>
        <div className="p-top"><span className="avatar">{initial(p.nameAsWritten)}</span><div><b>{p.nameAsWritten}</b><span className="sub">{p.age ?? '?'} · {s.shelter.name}</span></div></div>
        <div className="ptags">{[...p.flags, ...(card?.conditions ?? [])].map(x => <span key={x}>{nice(x)}</span>)}</div>
      </header>
      <main className="content" style={{ paddingTop: 20 }}>
        {photo && <figure className="photo" style={{ margin: 0 }}><img src={photo.url} alt={t.originalPhoto} /><span className="tagp">{t.originalPhoto}</span></figure>}
        <RouteNotice r={r} onCloud={retry} />
        {pending.length > 0 && <div className="list-h"><b>{t.toConfirm}</b><Badge source={pending[0].m.inferredBy} text={pending[0].m.inferredBy === 'device' ? t.readOnPhone : undefined} /></div>}
        {pending.map(({ m, i }) => (
          <div key={i} className="card amber rise">
            <b className="h" style={{ fontSize: 18 }}>“{m.nameAsWritten}” {m.strength}</b><span className="muted">{m.doseAsWritten} · {m.timesPerDay ?? '?'}×/day</span>
            {m.match.lookAlikes.length > 0 && <p className="note"><Icon name="alert" size={16} />{fmt(t.lookAlike, { names: m.match.lookAlikes.join(', ') })}</p>}
            {m.match.strengthKnown === false && <p className="note"><Icon name="alert" size={16} />{t.strengthUnknown}</p>}
            {m.match.status === 'unknown' && <p className="note"><Icon name="info" size={16} />{t.unknownName}</p>}
            <div className="chips">{[...(m.match.generic ? [m.match.generic] : []), ...m.match.candidates].map(g => (
              <span key={g} className="chips">
                <button className="btn small" onClick={() => f.careCard.confirmMed(id, i, g, 'family')}><Icon name="check" size={16} />{g} · {t.evacuees}</button>
                <button className="btn small" onClick={() => f.careCard.confirmMed(id, i, g, 'medic')}><Icon name="doctor" size={16} />{t.medic}</button>
              </span>))}</div>
          </div>
        ))}
        <div className="list-h"><b>{t.medicines}</b><span>{t.asWritten}</span></div>
        {confirmed.length === 0 && <p className="muted">{t.noMeds}</p>}
        {confirmed.map((m, i) => { const d = days(m); return (
          <div key={i} className="med rise" style={{ '--d': i } as CSSProperties}><span className="time">{(slots[String(m.timesPerDay ?? 1)] ?? ['—']).join(' ')}</span>
            <div className="tx"><b>{m.generic} {m.strength}</b><span>{m.doseAsWritten} · {fmt(t.confirmedBy, { who: m.confirmedBy! })}</span></div>
            {d != null && <span className={`left ${d <= f.rules.shortageDays ? 'low' : ''}`}>{fmt(t.nDays, { n: d })}</span>}</div>); })}
        <div className="row-card"><b>{t.allergies}</b><span>{card?.allergies?.length ? card.allergies.join(', ') : t.noneKnown}</span></div>
        {meds.length > 0 && <MedCheckCard personId={id} />}
        {meds.length > 0 && <FindOpen />}
        <button className="btn primary full" onClick={() => navigate(`#/staff/camera?mode=medicine&person=${id}`)}><Icon name="camera" />{t.addMedPhoto}</button>
      </main>
    </>
  );
}
function SpeakIcon({ id, text }: { id: string; text: string }) {
  const t = useT(), sp = useSpeaker(), on = sp.speaking(id);
  return <button className="icon-btn" aria-label={t.readAloud} aria-pressed={on} onClick={() => sp.toggle(id, text)}><Icon name={on ? 'stop' : 'speaker'} /></button>;
}

/** 1 · Medicine check: drug names + strengths only go to the cloud; the answer is for a pharmacist and is cross-checked offline. */
function MedCheckCard({ personId }: { personId: string }) {
  const t = useT(), f = useF(), s = useAppState();
  const [busy, setBusy] = useState(false);
  const c = s.cards.find(x => x.personId === personId)?.check;
  const [res, setRes] = useState('');
  const run = async () => { setBusy(true); setRes(await f.hybrid.checkMeds(personId)); setBusy(false); };
  const status = (name: string) => c?.cross.find(x => x.name === name);
  return (
    <div className="card rise">
      <div className="list-h" style={{ paddingTop: 0 }}><b><Icon name="sparkle" />{t.checkTitle}</b>{c && !c.pending && <Badge source="cloud" />}</div>
      {!c && <p className="note"><Icon name="lock" size={16} />{t.checkWhy}</p>}
      {c?.pending && <p className="note"><Icon name="clock" size={16} />{t.checkWaiting}</p>}
      {res === 'blocked' && <p className="note" role="alert"><Icon name="lock" size={16} />{t.blocked}</p>}
      {res === 'failed' && <p className="note" role="alert">{t.cloudFailed}</p>}
      {c && !c.pending && <>
        {c.items.map(it => { const x = status(it.name); return (
          <div key={it.name} className="check-item">
            <b>{it.name}{it.generic && it.generic.toLowerCase() !== it.name.toLowerCase() ? ` → ${it.generic}` : ''}</b>
            <div className="pills">
              {x?.status === 'agree' && <span className="pill ok"><Icon name="check" size={14} />{t.agree}</span>}
              {x?.status === 'mismatch' && <span className="pill warn"><Icon name="alert" size={14} />{fmt(t.mismatch, { offline: x.offline ?? '' })}</span>}
              {x?.status === 'cloud_only' && <span className="pill warn"><Icon name="info" size={14} />{t.cloudOnly}</span>}
              {it.cantMiss && <span className="pill urgent">{t.cantMiss}</span>}
            </div>
            {!!it.alsoKnownAs?.length && <span className="why">{t.alsoKnown}: {it.alsoKnownAs.join(', ')}</span>}
            {!!it.swaps?.length && <span className="why">{t.swaps}: {it.swaps.join(', ')}</span>}
            {it.note && <span className="why">{it.note}</span>}
          </div>); })}
        <div className="check-item"><b>{t.interactions}</b>
          {c.interactions.length === 0 ? <span className="why">{t.noInteractions}</span> : c.interactions.map((x, i) => (
            <span key={i} className="why"><span className={`pill ${x.severity === 'high' ? 'urgent' : 'warn'}`}>{x.severity}</span> {x.a} + {x.b}{x.note ? `: ${x.note}` : ''}</span>))}</div>
        <p className="note"><Icon name="doctor" size={16} />{t.forPharmacist}</p>
      </>}
      <button className="btn" disabled={busy} onClick={run}><Icon name="cloud" className={busy ? 'spin' : ''} />{t.checkCloud}</button>
    </div>
  );
}

/** 2 · Find it open: Grounding with Google Maps near a rounded location. Only places Google Maps returned are shown. */
function FindOpen() {
  const t = useT(), f = useF(), s = useAppState();
  const [busy, setBusy] = useState('');
  const [res, setRes] = useState('');
  const o = s.open;
  const run = async (need: string) => { setBusy(need); setRes(await f.hybrid.findOpen(need)); setBusy(''); };
  return (
    <div className="card rise">
      <div className="list-h" style={{ paddingTop: 0 }}><b><Icon name="clock" />{t.findOpen}</b>{o && !o.pending && <Badge source="maps" />}</div>
      <p className="note"><Icon name="lock" size={16} />{t.findOpenWhy}</p>
      <div className="chips">{(f.rules.findOpen.needs as string[]).map(n => (
        <button key={n} className="chip toggle" aria-pressed={o?.need === n} disabled={!!busy} onClick={() => run(n)}>{busy === n && <Icon name="sync" size={14} className="spin" />}{(t as Record<string, string>)[`need_${n}`] ?? nice(n)}</button>))}</div>
      {res === 'no_location' && <p className="note">{t.noLocation}</p>}
      {res === 'failed' && <p className="note" role="alert">{t.cloudFailed}</p>}
      {o?.pending && <p className="note"><Icon name="clock" size={16} />{t.openWaiting}</p>}
      {o && !o.pending && o.places.length === 0 && <p className="note">{t.noOpenFound}</p>}
      {o && !o.pending && o.places.map(p => (
        <div key={p.name} className="place"><span className="avatar sm" style={{ background: p.openNow ? 'var(--sage)' : '#F1EEE8', color: 'var(--sage-ink)' }}><Icon name={o.need === 'pharmacy' ? 'medicine' : 'doctor'} /></span>
          <div className="tx"><b>{p.name}</b><span>{[p.openNow === true ? t.openNow : p.openNow === false ? t.closedNow : null, p.hours, p.distanceKm != null ? fmt(t.kmAway, { km: p.distanceKm }) : null].filter(Boolean).join(' · ')}</span></div>
          {p.uri && <a className="round-btn ghost" href={p.uri} target="_blank" rel="noreferrer" aria-label={t.openInMaps}><Icon name="external" /></a>}</div>))}
    </div>
  );
}

// ---------- Medicine round ----------
function Round() {
  const t = useT(), s = useAppState(), f = useF();
  const sch = f.views.schedule();
  const name = (id: string) => s.people.find(p => p.id === id)?.nameAsWritten ?? id;
  return (
    <>
      <Top title={t.medicineRound} />
      <main className="content">
        {sch.alerts.map(a => <button key={a.personId + a.generic} className="task urgent" onClick={() => go(`person/${a.personId}`)}><span className="sq"><Icon name="medicine" /></span>
          <span className="tx"><span className="tagline">{t.cantMiss}</span><b>{name(a.personId)} · {a.generic}</b><span>{fmt(t.onlyDaysLeft, { n: a.supplyDays ?? '?' })}</span></span><span className="act plain"><Icon name="chevron" /></span></button>)}
        {Object.keys(sch.slots).length === 0 && <Empty>{t.noMeds}</Empty>}
        {Object.entries(sch.slots).map(([slot, list], si) => (
          <div key={slot} className="card rise" style={{ '--d': si } as CSSProperties}>
            <div className="slot-head"><span className="time">{slot}</span><span className="pill">{list.length}</span></div>
            {list.map(e => { const st = s.round[f.medicine.key(slot, e.personId, e.generic)]; return (
              <div key={e.personId + e.generic} className={`round-row ${st ?? ''}`}>
                <span className="grow"><b>{name(e.personId)}</b><small>{e.generic} · {e.doseAsWritten}</small></span>
                <span className="mark">
                  <button aria-pressed={st === 'taken'} aria-label={t.given} onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'taken')}><Icon name="check" /></button>
                  <button aria-pressed={st === 'refused'} aria-label={t.refused} onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'refused')}><Icon name="x" /></button>
                  <button aria-pressed={st === 'missing'} aria-label={t.missing} onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'missing')}><Icon name="missing" /></button>
                </span></div>); })}
          </div>
        ))}
        {sch.shortages.length > 0 && <div className="card"><h3><Icon name="package" />{t.stock}</h3>
          {sch.shortages.map(x => <p key={x.generic + x.strength}>{fmt(t.outOf, { n: x.people, drug: `${x.generic} ${x.strength ?? ''}`, days: f.rules.shortageDays })}</p>)}
          <button className="btn primary" onClick={f.medicine.requestSupply}><Icon name="send" />{t.requestSupplies}</button>
          <p className="note"><Icon name="lock" size={16} />{t.countsOnly}</p></div>}
        <FindOpen />
        <p className="muted">{drugs._notice}</p>
      </main>
    </>
  );
}

// ---------- Map · Field check: red pins are who to visit first, with the reasons ----------
function FieldMap() {
  const t = useT(), s = useAppState(), f = useF();
  const rows = f.views.field();
  const sel0 = location.hash.split('/')[3]?.split('?')[0];
  const [sel, setSel] = useState<string | null>(sel0 ?? (rows.find(r => r.status === 'unchecked' && r.pickup) ?? rows.find(r => r.status === 'unchecked'))?.id ?? null);
  const [scan, setScan] = useState(false);
  const sh = pack.shelters.find(x => x.id === s.shelter.id) ?? pack.shelters[0];
  const loc = (id: string) => s.field.find(e => e.id === id)?.location;
  const color = { P1: '#C4402B', P2: '#8A5A00', P3: '#5A6170' } as Record<string, string>;
  const row0 = rows.find(r => r.id === sel), hp = row0?.higher && pack.shelters.find(x => x.id === row0.higher!.id);
  const markers = [
    ...(hp ? [{ id: 'higher', lat: hp.lat, lng: hp.lng, icon: 'higher', color: '#2F7A52', label: t.higherPlace }] : []),
    { id: 'shelter', lat: sh.lat, lng: sh.lng, icon: 'home', color: '#1C2F4A', label: s.shelter.name },
    ...rows.flatMap(r => { const l = loc(r.id); return l ? [{ id: r.id, ...l, icon: r.status === 'unchecked' ? (r.tier === 'P1' ? 'mobility' : 'pin') : 'check', color: r.status === 'unchecked' ? color[r.tier] : '#2F7A52', label: r.name.length > 16 ? r.name.slice(0, 15) + '…' : r.name, halo: r.tier === 'P1' && r.status === 'unchecked', sel: sel === r.id, onClick: () => setSel(r.id) }] : []; }),
  ];
  const row = rows.find(r => r.id === sel);
  const p1 = rows.filter(r => r.tier === 'P1' && r.status === 'unchecked').length;
  const hours = (id: string) => { const e = s.field.find(x => x.id === id); return e?.lastContactAt == null ? t.never : fmt(t.hAgo, { n: Math.round((Date.now() - e.lastContactAt) / 3600_000) }); };
  return (
    <>
      <ShelterMap aspect={1} className="full" markers={markers}
        chips={<><span className="chip"><i style={{ background: '#C4402B' }} />{t.visitFirst} · {p1}</span><button className="chip" onClick={() => setScan(!scan)}><Icon name="qr" size={16} />{t.scanPickup}</button></>} />
      {scan && <div className="content" style={{ paddingBottom: 0 }}><QrScanner onText={x => { const p = decodePickupQr(x); if (p) { f.first.importPickup(p); setSel(`pk-${p.id}`); setScan(false); } return !!p; }} /></div>}
      <div className="sheet" style={{ paddingBottom: 120 }}>
        <span className="grab" />
        {row ? <>
          <div className="place"><span className="avatar rose">{initial(row.name)}</span>
            <div className="tx"><span className={`prio ${row.tier.toLowerCase()}`}>{row.pickup ? t.pickupFirst : row.tier === 'P1' ? t.visitFirst.toUpperCase() : row.tier}</span>
              <b className="h" style={{ fontSize: 22, marginTop: 4 }}>{row.name}{row.person.age ? `, ${row.person.age}` : ''}</b>
              <span>{[row.distanceM != null ? `${row.distanceM} m` : null, row.pickup ? fmt(t.askedWhen, { when: fmt(t.minAgo, { n: Math.max(0, Math.round((Date.now() - row.pickup.at) / 60_000)) }) }) : fmt(t.lastChecked, { when: hours(row.id) })].filter(Boolean).join(' · ')}</span></div>
            {row.needsReview && <Badge draft />}</div>
          <div className="risk">{row.pickup?.needs.map(n => <span key={n} className="pill urgent">{(t as Record<string, string>)[`need_${n}`] ?? n}</span>)}{row.why.map(w => <span key={w} className="pill urgent">{w}</span>)}</div>
          {row.higher && <div className="higher"><Icon name="higher" />{fmt(t.higherFor, { name: row.higher.name, m: row.higher.distanceM, dir: row.higher.direction })}</div>}
          <div className="cta" style={{ gridTemplateColumns: '1fr 1fr' }}>
            {row.pickup ? <button className="btn primary" onClick={() => f.field.visited(row.id, 'moved_to_shelter')}><Icon name="check" />{t.pickedUp}</button> :
            <button className="btn" onClick={() => { f.field.visited(row.id, 'safe_home'); setSel(rows.find(r => r.id !== row.id && r.status === 'unchecked')?.id ?? null); }}><Icon name="check" />{t.safeAtHome}</button>}
            <button className="btn danger" onClick={() => f.field.visited(row.id, 'needs_evacuation')}><Icon name="evacuate" />{t.needsEvacuation}</button>
          </div>
          <button className="btn primary" onClick={() => navigate('#/staff/camera?mode=report')}><Icon name="mic" />{t.report}</button>
          <p className="note"><Icon name="info" size={16} />{fmt(t.hazardNote, { event: f.rules.currentEvent })}</p>
        </> : <Empty mood="idle">{t.allChecked}</Empty>}
        <FindOpen />
      </div>
    </>
  );
}

// ---------- Check-in at the door: one spoken sentence (or their card) per person, understood on this phone ----------
function CheckIn() {
  const t = useT(), s = useAppState(), f = useF();
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [scan, setScan] = useState(false);
  const [c, setC] = useState<(Awaited<ReturnType<typeof f.first.understandCheckin>> & { card?: boolean }) | null>(null);
  const [done, setDone] = useState<{ id: string; name: string; flags: string[] } | null>(null);
  const count = s.people.filter(p => p.confirmedBy).length;
  const reachFlags = f.rules.reachFlags as string[];
  const dup = c ? f.first.alreadyHere(c.name, c.age) : [];
  const confirm = () => { if (!c) return; const id = f.first.checkin(c); setDone({ id, name: c.name, flags: c.flags }); setC(null); setText(''); };
  return (
    <>
      <header className="screen-top"><b>{t.checkIn}</b><span className="badge local"><Icon name="device" size={15} />{t.readsOnPhone}</span></header>
      <main className="content">
        <div className="count"><div><b>{count}</b><span> {t.checkedIn}</span></div><span>{new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span></div>
        <div className="compose">
          <textarea rows={2} value={text} placeholder={t.checkinHint} aria-label={t.checkIn} onChange={e => { setText(e.target.value); setC(null); }} />
          <div className="tools"><MicButton onText={x => { setText(x); setC(null); }} />
            <button className="round-btn ghost" aria-label={t.scanCard} onClick={() => setScan(!scan)}><Icon name="qr" /></button>
            <button className="btn primary" style={{ flex: 1 }} disabled={!text.trim() || busy} onClick={async () => { setBusy(true); setC(await f.first.understandCheckin(text)); setBusy(false); }}><Icon name="sparkle" className={busy ? 'spin' : ''} />{t.add}</button></div>
        </div>
        {scan && <QrScanner onText={x => { const card = decodeCheckinQr(x); if (card) { setC({ ...card, by: 'keywords', card: true }); setScan(false); } return !!card; }} />}
        {c && <div className="card rise">
          <div className="field-row"><label className="field">Name<input value={c.name} onChange={e => setC({ ...c, name: e.target.value })} /></label>
            <label className="field">Age<input inputMode="numeric" value={c.age ?? ''} onChange={e => setC({ ...c, age: e.target.value ? Number(e.target.value.replace(/\D/g, '')) : null })} /></label></div>
          <div className="chips">{FLAGS.map(fl => <button key={fl} className={`chip toggle ${c.flags.includes(fl) ? 'on' : ''}`} onClick={() => setC({ ...c, flags: c.flags.includes(fl) ? c.flags.filter(x => x !== fl) : [...c.flags, fl] })}>{nice(fl)}</button>)}</div>
          {dup.length > 0 && (
            <div className="dup" role="alert"><b><Icon name="warning" size={18} />{t.maybeHere}</b>
              {dup.map(p => <div key={p.id} className="dup-row"><span>{p.nameAsWritten}{p.age != null ? `, ${p.age}` : ''}</span>
                <button className="btn small" onClick={() => { setC(null); setText(''); navigate(`#/staff/person/${p.id}`); }}>{t.samePerson}</button></div>)}
            </div>)}
          {c.card ? <span className="badge kw"><Icon name="qr" size={15} />{t.fromCard}</span> : <span className={`badge ${c.by === 'device' ? 'local' : 'kw'}`}><Icon name={c.by === 'device' ? 'device' : 'keywords'} size={15} />{c.by === 'device' ? t.readOnPhone : t.keywordsOnly}</span>}
          <div className="cta"><button className="btn" onClick={() => setC(null)}><Icon name="x" />{t.close}</button><button className="btn primary" disabled={!c.name.trim()} onClick={confirm}><Icon name="check" />{dup.length ? t.notSame : t.confirmNext}</button></div>
        </div>}
        {done && <div className="card rise"><b className="h" style={{ fontSize: 18 }}>{done.name}</b>
          <div className="auto">{done.flags.some(x => reachFlags.includes(x)) && <div><Icon name="check" size={18} />{t.addedTell}</div>}
            {done.flags.includes('needs_meds') && <button className="btn link" onClick={() => navigate(`#/staff/camera?mode=medicine&person=${done.id}`)}><Icon name="medicine" size={18} />{t.addedMeds}</button>}</div></div>}
        <button className="btn" onClick={() => navigate('#/staff/camera?mode=list')}><Icon name="camera" />{t.paperList}</button>
      </main>
    </>
  );
}

// ---------- Announce: speak or type once, see exactly what evacuees see ----------
function Announce() {
  const t = useT(), s = useAppState(), f = useF();
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [list, setList] = useState(false);
  const a = s.announcements[0];
  const reach = f.views.reach(a?.id);
  const name = (id: string) => s.people.find(p => p.id === id)?.nameAsWritten ?? id;
  const h = a && headline(a);
  return (
    <>
      <header className="screen-top"><button className="round-btn" aria-label={t.back} onClick={() => go('now')}><Icon name="back" /></button><b>{t.announce}</b><a className="chip" href="#/big"><Icon name="announce" size={16} />Big Screen</a></header>
      <main className="content">
        <div className="compose">
          <textarea rows={2} value={text} placeholder={t.typeOrSpeak} aria-label={t.typeOrSpeak} onChange={e => setText(e.target.value)} />
          <div className="tools"><MicButton onText={setText} /><button className="btn primary" style={{ flex: 1 }} disabled={!text.trim() || busy} onClick={async () => { setBusy(true); await f.announcements.create(text.trim()); setText(''); setBusy(false); }}>
            <Icon name="sparkle" className={busy ? 'spin' : ''} />{t.makeCards}</button></div>
        </div>
        {a && h && <>
          <span className="preview-label">{t.whatEveryoneSees}</span>
          <div className="mini-news rise" key={a.id}><span className="ico"><Icon name={h.icon} size={34} /></span><div className="grow"><b>{h.title}</b><span>{[h.time, h.place].filter(Boolean).join(' · ')}</span></div></div>
          {(a.cards ?? []).slice(1).map((c, i) => <div key={i} className="mini-news" style={{ padding: 12 }}><span className="ico" style={{ width: 48, height: 48 }}><Icon name={c.icon} /></span><div className="grow"><b style={{ fontSize: 22 }}>{c.title}</b><span>{[c.time, c.place].filter(Boolean).join(' · ')}</span></div></div>)}
          <div className="chips"><Badge source={a.source} draft={a.draft} /><span className="muted">“{a.text}”</span></div>
          {reach.list.length > 0 && <button className="reach" onClick={() => setList(!list)} aria-expanded={list}>
            <span className="stack">{reach.list.filter(r => !r.told).slice(0, 3).map((r, i) => <span key={r.id} style={{ background: `var(--${TONES[i]})`, color: `var(--${TONES[i]}-ink)` }}>{initial(name(r.id))}</span>)}
              {reach.remaining > 3 && <span style={{ background: 'var(--lav)', color: 'var(--lav-ink)' }}>+{reach.remaining - 3}</span>}</span>
            <span className="grow"><b className="h" style={{ fontSize: 16 }}>{fmt(t.nToTell, { n: reach.remaining })}</b><br /><span className="muted">{t.tellWhy}</span></span><Icon name="chevron" /></button>}
          {list && <div className="card"><div className="progress"><i style={{ width: `${reach.list.length ? (100 * (reach.list.length - reach.remaining)) / reach.list.length : 0}%` }} /></div>
            {reach.list.map(r => <label key={r.id} className="check-row"><input type="checkbox" checked={r.told} onChange={() => f.announcements.toggleTold(a.id, r.id)} /><span className="box"><Icon name="check" size={18} sw={3} /></span>
              <span className="grow"><b className="h" style={{ fontSize: 17 }}>{name(r.id)}</b><br /><span className="muted">{r.reasons.map(nice).join(', ')}</span></span></label>)}</div>}
          <div className="cta">
            <button className="btn" onClick={() => print()}><Icon name="print" />{t.print}</button>
            {a.shown ? <span className="btn" style={{ background: 'var(--sage)', color: 'var(--sage-ink)', boxShadow: 'none' }}><Icon name="check" />{t.shown}</span>
              : <button className="btn primary" onClick={() => f.announcements.approve(a.id)}><Icon name="send" />{t.showEveryone}</button>}
          </div>
        </>}
        {!a && <Empty mood="idle">{t.noNews}</Empty>}
      </main>
    </>
  );
}

// ---------- Paper fallback (§6): roster, medicine round, tell-in-person. Black-and-white safe; the app is never the only copy. ----------
function PrintLists() {
  const t = useT(), s = useAppState(), f = useF();
  const here = s.people.filter(p => p.confirmedBy), sch = f.views.schedule();
  const ann = [...s.announcements].reverse().find(a => a.shown) ?? s.announcements[0], reach = f.views.reach(ann?.id);
  const name = (id: string) => s.people.find(p => p.id === id)?.nameAsWritten ?? id;
  const box = <span className="tick" aria-hidden />;
  return (
    <>
      <Top title={t.printLists} back="settings"><button className="btn primary small" onClick={() => print()}><Icon name="print" />{t.print}</button></Top>
      <main className="content print-lists">
        <h1><Kit mood="idle" size={56} />{t.printLists}</h1>
        <p className="muted">{fmt(t.printKeep, { time: new Date().toLocaleString() })}</p>
        <h2>{fmt(t.rosterTitle, { n: here.length })}</h2>
        <table><thead><tr><th>{t.colName}</th><th>{t.colAge}</th><th>{t.colNeeds}</th></tr></thead>
          <tbody>{here.map(p => <tr key={p.id}><td><b>{p.nameAsWritten}</b></td><td>{p.age ?? ''}</td><td>{p.flags.map(nice).join(', ')}</td></tr>)}</tbody></table>
        <h2>{t.medicineRound}</h2>
        {sch.alerts.length > 0 && <p><b>{t.cantMiss}:</b> {sch.alerts.map(a => `${name(a.personId)} · ${a.generic}`).join(' · ')}</p>}
        <table><thead><tr><th></th><th>{t.colName}</th><th>{t.colMedicine}</th><th>{t.given}</th></tr></thead>
          <tbody>{Object.entries(sch.slots).flatMap(([slot, list]) => list.map(e => <tr key={slot + e.personId + e.generic}><td><b>{slot}</b></td><td>{name(e.personId)}</td><td>{e.generic} · {e.doseAsWritten}</td><td>{box}</td></tr>))}</tbody></table>
        <h2>{fmt(t.tellInPerson, { n: reach.remaining })}</h2>
        <table><thead><tr><th>{t.colName}</th><th>{t.colNeeds}</th><th>{t.colTold}</th></tr></thead>
          <tbody>{reach.list.map(r => <tr key={r.id}><td><b>{name(r.id)}</b></td><td>{r.reasons.map(nice).join(', ')}</td><td>{r.told ? '✓' : box}</td></tr>)}</tbody></table>
      </main>
    </>
  );
}

// ---------- Signal Scout alert: the one loud moment ----------
function ScoutAlert() {
  const t = useT(), f = useF(), p = f.queue.progress();
  return (
    <div className="scout" role="alertdialog" aria-label={t.signalBackTitle}>
      <div className="halo"><Kit mood="alert" size={170} /></div>
      <h1>{t.signalBackTitle}</h1>
      <p>{t.bringPhones} {fmt(t.sentOf, p)}</p>
      <div className="progress"><i style={{ width: `${p.total ? (100 * p.sent) / p.total : 100}%` }} /></div>
      <button className="btn amber" style={{ minWidth: 220 }} onClick={f.scout.dismiss}>{t.gotIt}</button>
    </div>
  );
}

// ---------- Settings ----------
function Settings() {
  const s = useAppState(), f = useF(), t = useT();
  const [status, setStatus] = useState('');
  const [chunks, setChunks] = useState<string[] | null>(null);
  const [paste, setPaste] = useState('');
  const base = location.href.split('#')[0];
  return (
    <>
      <Top title={t.settings} />
      <main className="content">
        <div className="card"><h3><Icon name="download" />{t.shelterReady}</h3>
          <button className="btn primary" onClick={async () => setStatus(fmt(t.onDeviceModel, { status: await (f.prepareDevice?.() ?? Promise.resolve('demo')) }))}><Icon name="device" />{t.prepareDevice}</button>
          {status && <p className="muted">{status}</p>}</div>
        <div className="card"><h3><Icon name="globe" />{t.language}</h3>
          <div className="seg"><button aria-pressed={s.lang === 'en'} onClick={() => f.settings.setLang('en')}>English</button><button aria-pressed={s.lang === 'id'} onClick={() => f.settings.setLang('id')}>Bahasa Indonesia</button></div></div>
        <div className="card"><label className="switch-row"><Icon name="lock" /><span className="grow">{t.privateMode}</span>
          <input type="checkbox" role="switch" checked={s.privateMode} onChange={e => f.settings.setPrivate(e.target.checked)} /></label></div>
        <div className="card"><h3><Icon name="signal" />{t.signalScout}</h3>
          <p className="muted">{t.signal}: <b>{nice(s.signal)}</b> · {t.lastSeen}: {s.lastSignalAt ? new Date(s.lastSignalAt).toLocaleTimeString() : t.never}</p>
          <div className="cta" style={{ gridTemplateColumns: '1fr 1fr' }}><button className="btn small" onClick={() => f.scout.check()}><Icon name="signal" />{t.checkNow}</button>
            <button className="btn small" onClick={() => f.queue.flush()}><Icon name="sync" />{t.syncNow}</button></div>
          <ul className="jobs">{s.jobs.slice(-8).map(j => <li key={j.id}><span className={`dot ${j.state}`} />{nice(j.type)}<span className="muted">{j.state}{j.retries ? ` · retry ${j.retries}` : ''}</span></li>)}</ul></div>
        <EvalCard />
        <div className="card"><h3><Icon name="print" />{t.printLists}</h3><p className="muted">{t.printWhy}</p>
          <button className="btn primary" onClick={() => go('print')}><Icon name="print" />{t.printLists}</button></div>
        <div className="card"><h3><Icon name="qr" />{t.courier}</h3>
          <button className="btn small" onClick={() => setChunks(f.queue.exportChunks())}><Icon name="qr" />{t.exportQueue}</button>
          <div className="qr-row">{chunks?.map((c, i) => <figure key={i}><QR text={c} label={`${t.courier} ${i + 1}/${chunks.length}`} /><figcaption>{i + 1}/{chunks.length}</figcaption></figure>)}</div>
          <textarea rows={2} placeholder={t.importCodes} value={paste} onChange={e => setPaste(e.target.value)} />
          <button className="btn small" onClick={() => { const n = f.queue.importChunks(paste.split('\n').filter(Boolean)); setStatus(fmt(t.imported, { n })); setPaste(''); }}><Icon name="download" />{t.import}</button></div>
        <div className="card"><h3><Icon name="people" />{t.joinQrs}</h3>
          <div className="qr-row">
            <figure><QR text={`${base}#/staff/now?join=${s.shelter.id}&role=volunteer`} label={t.volunteer} size={130} /><figcaption>{t.volunteer}</figcaption></figure>
            <figure><QR text={`${base}#/staff/now?join=${s.shelter.id}&role=medic`} label={t.medic} size={130} /><figcaption>{t.medic}</figcaption></figure>
            <figure><QR text={`${base}#/public`} label={t.evacuees} size={130} /><figcaption>{t.evacuees}</figcaption></figure></div>
          <div className="chips"><a className="chip" href="#/big"><Icon name="announce" size={16} />Big Screen</a><a className="chip" href="#/public"><Icon name="people" size={16} />{t.evacuees}</a></div></div>
        <button className="btn" onClick={f.settings.loadDemo}><Icon name="demo" />{t.loadDemo}</button>
        {f.env === 'demo' && <p className="note"><Icon name="demo" size={16} />{t.demoBadge}</p>}
      </main>
    </>
  );
}
