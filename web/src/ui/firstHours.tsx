// Evacuee screens for the first hours: Go now -> Show the way -> I need pickup -> check-in card.
import { useState, type CSSProperties } from 'react';
import { encodeCheckinQr, encodePickupQr } from '../domain/firstHours';
import { distanceM } from '../domain/regionPack';
import { fmt } from './i18n';
import { Icon, Kit, MicButton, navigate, QR, SayButton, ShelterMap, useAppState, useF, useT } from './common';

const go = (p: string) => navigate(`#/public/${p}`);
const tr = (t: Record<string, string>, k: string, fb = k) => t[k] ?? fb;
const bearing = (a: { lat: number; lng: number }, b: { lat: number; lng: number }) => {
  const r = Math.PI / 180, dl = (b.lng - a.lng) * r;
  const y = Math.sin(dl) * Math.cos(b.lat * r), x = Math.cos(a.lat * r) * Math.sin(b.lat * r) - Math.sin(a.lat * r) * Math.cos(b.lat * r) * Math.cos(dl);
  return (Math.atan2(y, x) / r + 360) % 360;
};
function Top({ title, sub, back = '' }: { title: string; sub?: string; back?: string }) {
  const t = useT();
  return <header className="ev-top"><button className="round-btn" aria-label={t.back} onClick={() => go(back)}><Icon name="back" /></button><div className="grow"><b>{title}</b>{sub && <small>{sub}</small>}</div></header>;
}

/** 1 · Go now: any warning text -> plain words on the phone; the one higher place to go, from offline flood data. */
export function GoNow() {
  const t = useT(), f = useF(), s = useAppState();
  const [text, setText] = useState('');
  const [busy, setBusy] = useState('');
  const w = s.warning, dest = f.first.destination();
  const run = async (k: string, fn: () => Promise<unknown>) => { setBusy(k); await fn(); setBusy(''); };
  const tone = w?.level === 'go_now' ? 'rose' : 'honey';
  const say = w ? [tr(t, `hazard_${w.hazard}`), tr(t, `level_${w.level}`), w.plain, dest ? `${t.goHere}: ${dest.name}, ${dest.distanceM} m ${dest.direction}` : ''].join('. ') : '';
  return (
    <>
      {w ? (
        <header className={`warn-hero ${tone}`}>
          <div className="top"><button className="round-btn" aria-label={t.back} onClick={() => go('')}><Icon name="back" /></button><Kit mood={w.level === 'go_now' ? 'alert' : 'idle'} size={58} />
            <span className="eyebrow"><span className="live" />{tr(t, `hazard_${w.hazard}`)}</span></div>
          <h2>{w.plain}</h2>
          <p>{w.action ?? tr(t, `level_${w.level}`)} {t.takeMeds}</p>
          <span className={`badge ${w.by === 'device' ? 'local' : w.by === 'cloud' ? 'cloud' : 'kw'}`}><Icon name={w.by === 'cloud' ? 'cloud' : w.by === 'device' ? 'device' : 'keywords'} size={15} />{w.by === 'device' ? t.plainOnPhone : w.by === 'cloud' ? t.officialCloud : t.plainByRules}</span>
          {w.sources.map(x => <a key={x.uri} className="src" href={x.uri} target="_blank" rel="noreferrer">{x.title ?? x.uri}</a>)}
        </header>
      ) : <Top title={t.goSafety} sub={t.goSafetySub} />}
      <main className="content" style={{ paddingTop: 16 }}>
        {w && (dest ? (
          <div className="dest rise"><span className="lbl">{t.goHere}</span><b>{dest.name}</b>
            <div className="meta2"><span>{dest.distanceM} m {dest.direction}</span><span>{fmt(t.higherBy, { m: dest.handGain })}</span></div>
            <span className="muted">{fmt(t.fromHere, { where: dest.gps ? t.myLocation : t.theShelter })}</span></div>
        ) : <p className="note"><Icon name="info" size={16} />{t.noHigher}</p>)}
        {w && <>
          <button className="big-btn b-sky rise" onClick={() => go('way')} disabled={!dest}><span className="disc"><Icon name="nav" size={32} /></span><b>{t.showWay}</b><span className="go"><Icon name="chevron" /></span></button>
          <button className="big-btn b-rose rise" onClick={() => go('pickup')}><span className="disc"><Icon name="mobility" size={32} /></span><b>{t.needPickup}<small>{t.needPickupSub}</small></b><span className="go"><Icon name="chevron" /></span></button>
          <SayButton id="warn" text={say} />
        </>}
        <div className="ask-box">
          <b className="h" style={{ fontSize: 20, color: 'var(--navy)' }}>{t.gotWarning}</b>
          <textarea rows={3} value={text} placeholder={t.pasteWarning} aria-label={t.pasteWarning} onChange={e => setText(e.target.value)} />
          <div className="tools"><MicButton onText={setText} /><button className="btn primary" style={{ flex: 1 }} disabled={!text.trim() || !!busy} onClick={() => run('plain', () => f.first.warn(text))}><Icon name="sparkle" className={busy === 'plain' ? 'spin' : ''} />{t.makePlain}</button></div>
          {f.env === 'demo' && <button className="btn link" onClick={() => setText(t.exampleText)}>{t.exampleWarning}</button>}
        </div>
        <div className="cta" style={{ gridTemplateColumns: '1fr 1fr' }}>
          <button className="btn small" onClick={() => navigator.geolocation?.getCurrentPosition(p => f.first.setPos({ lat: p.coords.latitude, lng: p.coords.longitude }))}><Icon name="pin" />{t.useMyLocation}</button>
          <button className="btn small" disabled={s.signal !== 'back' || !!busy} onClick={() => run('latest', () => f.first.latest())}><Icon name="cloud" className={busy === 'latest' ? 'spin' : ''} />{t.checkOfficial}</button>
        </div>
      </main>
    </>
  );
}

// Live GPS + compass, attached with a ref callback (no useEffect); stops when the screen closes.
let heading: number | null = null, lastBuzz = 0;
const headingListeners = new Set<() => void>();
export function Way() {
  const t = useT(), f = useF(), s = useAppState();
  const [, tick] = useState(0);
  const dest = f.first.destination();
  const attach = (el: HTMLDivElement | null) => {
    if (!el) return;
    const id = navigator.geolocation?.watchPosition(p => f.first.setPos({ lat: p.coords.latitude, lng: p.coords.longitude }), () => {}, { enableHighAccuracy: true });
    const ori = (e: DeviceOrientationEvent & { webkitCompassHeading?: number }) => {
      heading = e.webkitCompassHeading ?? (e.absolute && e.alpha != null ? 360 - e.alpha : null);
      headingListeners.forEach(l => l());
    };
    const l = () => tick(n => n + 1);
    headingListeners.add(l);
    addEventListener('deviceorientationabsolute', ori as EventListener); addEventListener('deviceorientation', ori as EventListener);
    return () => { if (id != null) navigator.geolocation.clearWatch(id); headingListeners.delete(l); removeEventListener('deviceorientationabsolute', ori as EventListener); removeEventListener('deviceorientation', ori as EventListener); };
  };
  if (!dest) return <><Top title={t.showWay} back="go" /><main className="content"><p className="note">{t.noHigher}</p></main></>;
  const from = dest.from, m = Math.round(distanceM(from.lat, from.lng, dest.lat, dest.lng));
  const b = bearing(from, dest), rot = heading == null ? b : b - heading;
  const off = heading != null && Math.abs(((rot + 540) % 360) - 180) > 60;
  if (off && Date.now() - lastBuzz > 8000) { lastBuzz = Date.now(); navigator.vibrate?.([200, 100, 200]); }
  const arrived = m <= f.rules.arrivedM;
  return (
    <div ref={attach} style={{ display: 'contents' }}>
      <Top title={dest.name} back="go" />
      <div style={{ position: 'relative' }}>
        <ShelterMap aspect={0.85} you={from} markers={[{ id: 'dest', lat: dest.lat, lng: dest.lng, icon: 'higher', color: '#2F7A52', label: fmt(t.higherBy, { m: dest.handGain }) }]} />
        <div className="navbar"><span className="arr" style={{ '--rot': `${rot}deg` } as CSSProperties}><Icon name="nav" size={38} sw={2.6} /></span>
          <div><b>{arrived ? t.imHere : fmt(t.walkM, { m })}</b><span>{arrived ? '' : `${fmt(t.headDir, { dir: dest.direction })} · ${fmt(t.minutes, { n: Math.max(1, Math.round(m / 70)) })}`}</span></div></div>
      </div>
      <div className="sheet" style={{ marginTop: -24 }}>
        <span className="grab" />
        {!s.myPos && <p className="note"><Icon name="pin" size={16} />{t.noGps}</p>}
        {arrived ? <div className="toast"><Kit mood="idle" size={48} /><span>{t.arrived}</span></div> : <p className="note"><Icon name="vibrate" size={16} />{t.vibrateNote}</p>}
        <p className="note"><Icon name="info" size={16} />{t.directionNote}</p>
        <button className="btn primary full" onClick={() => go('checkin')}><Icon name="check" />{t.imHere}</button>
      </div>
    </div>
  );
}

/** 3 · I need pickup: spoken words understood on the phone; sent on tap, through the outbox (waits for signal); QR for a passing volunteer. */
export function PickupPage() {
  const t = useT(), f = useF(), s = useAppState();
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [p, setP] = useState<Awaited<ReturnType<typeof f.first.understandPickup>> | null>(null);
  const mine = s.myPickup, job = mine && s.jobs.find(j => j.id === mine.jobId);
  return (
    <>
      <Top title={t.needPickup} sub={t.sayWho} back="go" />
      <main className="content">
        {!mine && <>
          <div className="ask-box">
            <textarea rows={2} value={text} placeholder={t.pickupHint} aria-label={t.needPickup} onChange={e => { setText(e.target.value); setP(null); }} />
            <div className="tools"><MicButton onText={x => { setText(x); setP(null); }} /><button className="btn primary" style={{ flex: 1 }} disabled={!text.trim() || busy} onClick={async () => { setBusy(true); setP(await f.first.understandPickup(text)); setBusy(false); }}><Icon name="sparkle" className={busy ? 'spin' : ''} />{t.ask}</button></div>
          </div>
          {p && <div className="answer rise">
            <span className="eyebrow">{t.kitUnderstood}</span>
            <div className="kv"><span>{t.who}</span><span>{p.who} · {fmt(t.people_n, { n: p.people })}</span><span>{t.needs}</span><span>{[tr(t, `mob_${p.mobility}`), ...p.needs.map(n => tr(t, `need_${n}`))].join(' · ')}</span>
              <span>{t.where}</span><span>{s.myPos ? t.hereGps : t.theShelter}</span></div>
            <span className={`badge ${p.by === 'device' ? 'local' : 'kw'}`}><Icon name={p.by === 'device' ? 'device' : 'keywords'} size={15} />{p.by === 'device' ? t.understoodOnPhone : t.keywordsOnly}</span>
            {!s.myPos && <button className="btn small" onClick={() => navigator.geolocation?.getCurrentPosition(x => f.first.setPos({ lat: x.coords.latitude, lng: x.coords.longitude }))}><Icon name="pin" />{t.useMyLocation}</button>}
            <button className="btn primary" onClick={() => f.first.sendPickup(p)}><Icon name="send" />{t.sendPickup}</button>
          </div>}
        </>}
        {mine && <>
          <div className={`wait ${job?.state === 'sent' ? 'ok' : ''}`}><Icon name={job?.state === 'sent' ? 'check' : 'clock'} />{job?.state === 'sent' ? t.pickupSent : t.pickupWaiting}</div>
          <div className="qr-show"><QR text={encodePickupQr(mine)} size={240} label={t.showVolunteer} /><b className="h" style={{ fontSize: 20 }}>{mine.who}</b><span className="muted" style={{ textAlign: 'center' }}>{t.showVolunteer}</span></div>
        </>}
      </main>
    </>
  );
}

const SELF_FLAGS = ['deaf', 'hard_of_hearing', 'cant_walk', 'wheelchair', 'needs_meds', 'infant', 'pregnant'];
/** 4 · Check-in card: one QR at the door. People choose their own needs; the app never guesses a disability. */
export function CheckinCard() {
  const t = useT(), s = useAppState();
  const [name, setName] = useState(s.familyGroup?.me.name ?? '');
  const [age, setAge] = useState('');
  const [flags, setFlags] = useState<string[]>([]);
  const fam = s.familyGroup;
  return (
    <>
      <Top title={t.atShelter} sub={t.showAtDoor} />
      <main className="content">
        <div className="field-row"><label className="field">{t.yourName}<input value={name} onChange={e => setName(e.target.value)} /></label><label className="field">Age<input inputMode="numeric" value={age} onChange={e => setAge(e.target.value.replace(/\D/g, '').slice(0, 3))} /></label></div>
        {name.trim() && <div className="pass rise"><span className="qrbox"><QR text={encodeCheckinQr({ name, age: age ? Number(age) : null, flags })} size={200} label={t.checkinCard} /></span>
          <b>{name}</b>{fam && <span>{t.familyGroup}: {fam.members.length + 1}</span>}</div>}
        <span className="preview-label">{t.tellStaffOptional}</span>
        <div className="tick">{SELF_FLAGS.map(fl => <button key={fl} className={flags.includes(fl) ? 'on' : ''} aria-pressed={flags.includes(fl)} onClick={() => setFlags(flags.includes(fl) ? flags.filter(x => x !== fl) : [...flags, fl])}>{tr(t, `flag_${fl}`)}</button>)}</div>
        {(flags.includes('deaf') || flags.includes('hard_of_hearing')) && <div className="toast rise"><Kit mood="idle" size={48} /><span>{t.writeNotCall}</span></div>}
      </main>
    </>
  );
}
