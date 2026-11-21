import { useState } from 'react';
import drugs from '../../../shared/drugs/demo-drugs.json';
import type { RouteResult } from '../features/inferenceRouter';
import type { ImageInput } from '../ports';
import { Badge, Empty, ICON, PhotoButton, QR, RouteNotice, StatusBar, Tile, useAppState, useF, useT } from './common';
import { Kit } from './Kit';

type Tab = 'today' | 'people' | 'medicine' | 'announce' | 'field' | 'settings';

export function Staff({ tab, go }: { tab: Tab; go: (t: Tab) => void }) {
  const t = useT(), s = useAppState(), f = useF();
  const tabs: [Tab, string][] = [['today', '🏠'], ['people', '👥'], ['medicine', '💊'], ['announce', '📢'], ['field', '🏚']];
  return (
    <div className="app">
      <StatusBar title={s.shelter.name} />
      {s.scoutAlert && <ScoutAlert />}
      {s.signal === 'no_signal' && s.lastSignalAt == null && tab === 'today' && <div className="notice">🏮 {t.noSignal} {t.airplaneTip}</div>}
      <main>
        {tab === 'today' && <Today go={go} />}
        {tab === 'people' && <People />}
        {tab === 'medicine' && <Medicine />}
        {tab === 'announce' && <Announce />}
        {tab === 'field' && <Field />}
        {tab === 'settings' && <Settings />}
      </main>
      <nav className="tabs">
        {tabs.map(([k, i]) => <button key={k} aria-current={tab === k} onClick={() => go(k)}>{i}<br />{t[k]}</button>)}
        <button aria-label={t.settings} aria-current={tab === 'settings'} onClick={() => go('settings')}>⚙️</button>
      </nav>
      <footer className="counter">Today: {f.views.counts().device} on device · {f.views.counts().cloud} cloud · {s.jobs.filter(j => j.state !== 'sent').length} waiting</footer>
    </div>
  );
}

function ScoutAlert() {
  const t = useT(), f = useF(), p = f.queue.progress();
  return (
    <div className="scout-alert" role="alertdialog" aria-label={t.signalBack}>
      <Kit pose="wave" size={160} />
      <h1>{t.signalBack}</h1>
      <p>Sent {p.sent} of {p.total} items. {p.waiting} waiting for the next signal.</p>
      <button className="main" onClick={f.scout.dismiss}>OK</button>
    </div>
  );
}

function Today({ go }: { go: (t: Tab) => void }) {
  const t = useT(), s = useAppState(), f = useF();
  const sch = f.views.schedule(), reach = f.views.reach(), field = f.views.field();
  const nextSlot = Object.keys(sch.counts)[0];
  return (
    <div className="grid2">
      <Tile icon="💊" n={nextSlot ? sch.counts[nextSlot] : 0} label={`${t.medicine} ${nextSlot ?? ''}`} onClick={() => go('medicine')} />
      <Tile icon="⚠️" n={sch.alerts.length} label={t.cantMiss} danger={sch.alerts.length > 0} onClick={() => go('medicine')} />
      <Tile icon="📢" n={reach.remaining} label="to tell" onClick={() => go('announce')} />
      <Tile icon="🏚" n={field.filter(r => r.tier === 'P1').length} label="P1 visits" danger onClick={() => go('field')} />
      <Tile icon="🙋" n={s.help.filter(h => !h.done).length} label={t.help} onClick={() => go('people')} />
      <Tile icon="👥" n={s.people.length} label={t.people} onClick={() => go('people')} />
      {s.help.filter(h => !h.done).map(h => (
        <div key={h.id} className="card wide">🙋 {h.kind} · 📍 {h.spot} <button onClick={() => f.settings.helpDone(h.id)}>✓</button></div>
      ))}
    </div>
  );
}

function People() {
  const t = useT(), s = useAppState(), f = useF();
  const [photo, setPhoto] = useState<{ img: ImageInput; url: string } | null>(null);
  const [r, setR] = useState<RouteResult | null>(null);
  const [q, setQ] = useState('');
  const scan = async (img: ImageInput, url: string, cloud = false) => { setPhoto({ img, url }); setR(await f.roster.scan(img, cloud)); };
  const review = s.people.filter(p => !p.confirmedBy);
  const list = s.people.filter(p => p.confirmedBy && p.nameAsWritten.toLowerCase().includes(q.toLowerCase()));
  return (
    <section>
      {photo && <img className="source" src={photo.url} alt="Original list photo" />}
      <RouteNotice r={r} onCloud={() => photo && scan(photo.img, photo.url, true)} />
      {s.batch.length > 0 && <div className="notice">⏳ {s.batch.length} photo(s) waiting <button onClick={() => f.roster.processBatch()}>▶ Process now</button></div>}
      {review.map(p => (
        <div key={p.id} className={`card ${p.needsReview ? 'uncertain' : ''}`}>
          <Badge source={p.inferredBy === 'staff' ? undefined : p.inferredBy} draft={p.needsReview} />
          <PersonForm p={p} />
        </div>
      ))}
      <input className="search" placeholder={`🔍 ${t.search}`} value={q} onChange={e => setQ(e.target.value)} />
      <ul className="list">
        {list.map(p => <li key={p.id}><b>{p.nameAsWritten}</b>, {p.age ?? '?'} · {p.flags.join(', ')} {p.flags.includes('deaf') || p.flags.includes('hard_of_hearing') ? '🧏' : ''}</li>)}
      </ul>
      <PhotoButton label={t.scanList} onPhoto={(img, url) => scan(img, url)} />
    </section>
  );
}

const FLAGS = ['hard_of_hearing', 'deaf', 'needs_meds', 'wheelchair', 'cant_walk', 'lives_alone', 'foreign_language', 'infant', 'pregnant', 'elderly', 'dementia'];

function PersonForm({ p }: { p: ReturnType<typeof useAppState>['people'][number] }) {
  const t = useT(), f = useF();
  const [name, setName] = useState(p.nameAsWritten), [age, setAge] = useState(p.age?.toString() ?? ''), [flags, setFlags] = useState(p.suggestedFlags);
  return (
    <div>
      <label>Name (as written) <input value={name} onChange={e => setName(e.target.value)} /></label>
      <label>Age <input inputMode="numeric" value={age} onChange={e => setAge(e.target.value)} /></label>
      {p.notes && <p className="muted">📝 {p.notes}</p>}
      <div className="chips">{FLAGS.map(fl => (
        <label key={fl} className="chip"><input type="checkbox" checked={flags.includes(fl)} onChange={e => setFlags(e.target.checked ? [...flags, fl] : flags.filter(x => x !== fl))} />{fl.replace(/_/g, ' ')}</label>
      ))}</div>
      <button className="main" onClick={() => f.roster.confirm(p.id, { nameAsWritten: name, age: age ? Number(age) : null, flags })}>✓ {t.confirm}</button>
    </div>
  );
}

function Medicine() {
  const t = useT(), s = useAppState(), f = useF();
  const sch = f.views.schedule();
  const name = (id: string) => s.people.find(p => p.id === id)?.nameAsWritten ?? id;
  const [pid, setPid] = useState(s.people[0]?.id ?? '');
  const [r, setR] = useState<RouteResult | null>(null);
  const [photo, setPhoto] = useState<{ img: ImageInput; url: string } | null>(null);
  const read = async (img: ImageInput, url: string, cloud = false) => { setPhoto({ img, url }); setR(await f.careCard.read(pid, img, cloud)); };
  const pending = s.cards.flatMap(c => c.meds.map((m, i) => ({ c, m, i }))).filter(x => !x.m.confirmedBy);
  return (
    <section>
      {sch.alerts.map(a => (
        <div key={a.personId + a.generic} className="card danger">⚠️ {t.cantMiss}: <b>{name(a.personId)}</b> · {a.generic} ({a.type}) · supply left: {a.supplyDays ?? '?'} day(s)</div>
      ))}
      {Object.keys(sch.slots).length === 0 && <Empty>{t.noMeds}</Empty>}
      {Object.entries(sch.slots).map(([slot, list]) => (
        <div key={slot} className="card"><h3>{slot} · {list.length}</h3>
          {list.map(e => {
            const st = s.round[f.medicine.key(slot, e.personId, e.generic)];
            return (
              <div key={e.personId + e.generic} className="row">
                <span><b>{name(e.personId)}</b> · {e.generic} · {e.doseAsWritten} {st && <em>({st})</em>}</span>
                <span>
                  <button onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'taken')}>✓ {t.given}</button>
                  <button onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'refused')}>✗</button>
                  <button onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'missing')}>∅</button>
                </span>
              </div>);
          })}
        </div>
      ))}
      {sch.shortages.length > 0 && <div className="card"><h3>Stock</h3>
        {sch.shortages.map(x => <div key={x.generic + x.strength}>{x.people} person(s) out of <b>{x.generic} {x.strength}</b> within {f.rules.shortageDays} days</div>)}
        <button className="main" onClick={f.medicine.requestSupply}>📦 {t.requestSupplies}</button>
        <p className="muted">Counts by generic name only. No names leave the device.</p></div>}
      {photo && <img className="source" src={photo.url} alt="Original medicine photo" />}
      <RouteNotice r={r} onCloud={() => photo && read(photo.img, photo.url, true)} />
      {pending.map(({ c, m, i }) => (
        <div key={c.personId + i} className="card uncertain">
          <Badge source={m.inferredBy} draft /> <b>{name(c.personId)}</b>: “{m.nameAsWritten}” {m.strength} · {m.doseAsWritten} · {m.timesPerDay ?? '?'}×/day · left {m.qtyLeft ?? '?'}
          {m.match.lookAlikes.length > 0 && <p className="warn">⚠️ Look-alike names: {m.match.lookAlikes.join(', ')}. Check the label.</p>}
          {m.match.strengthKnown === false && <p className="warn">⚠️ Strength not in the list: needs a pharmacist.</p>}
          {m.match.status === 'unknown' && <p className="warn">❓ Unknown name: needs a pharmacist. Never guessed.</p>}
          {[...(m.match.generic ? [m.match.generic] : []), ...m.match.candidates].map(g => (
            <span key={g}>
              <button onClick={() => f.careCard.confirmMed(c.personId, i, g, 'self')}>✓ {g} (evacuee)</button>
              <button onClick={() => f.careCard.confirmMed(c.personId, i, g, 'medic')}>✓ {g} (medic)</button>
            </span>
          ))}
        </div>
      ))}
      <p className="muted">{drugs._notice}</p>
      <label>Person <select value={pid} onChange={e => setPid(e.target.value)}>{s.people.map(p => <option key={p.id} value={p.id}>{p.nameAsWritten}</option>)}</select></label>
      <PhotoButton label="Medicine photo" onPhoto={(img, url) => read(img, url)} />
    </section>
  );
}

function Announce() {
  const t = useT(), s = useAppState(), f = useF();
  const [text, setText] = useState('');
  const a = s.announcements[0];
  const reach = f.views.reach(a?.id);
  const name = (id: string) => s.people.find(p => p.id === id)?.nameAsWritten ?? id;
  return (
    <section>
      <textarea rows={3} value={text} placeholder="Makan siang jam 12.00 di aula / Food at 3 pm at the gym entrance" onChange={e => setText(e.target.value)} />
      <button className="main" disabled={!text.trim()} onClick={async () => { await f.announcements.create(text.trim()); setText(''); }}>✍️ Make cards</button>
      {a && <div className="card">
        <p className="muted">Original: “{a.text}”</p>
        <Badge source={a.source} draft={a.draft} />
        <div className="preview">
          <Kit pose="card" size={56} />
          {(a.cards ?? [{ icon: a.icons[0] ?? 'info', title: a.text.slice(0, 40), time: a.times[0], place: a.place }]).map((c, i) => (
            <div key={i} className="kcard"><span className="ic">{ICON[c.icon] ?? 'ℹ️'}</span> <b>{c.title}</b> {c.time && <>· {c.time}</>} {c.place && <div>📍 {c.place}</div>}</div>
          ))}
          {a.cards == null && a.icons.length > 1 && <div>{a.icons.map(i => ICON[i]).join(' ')}</div>}
        </div>
        {a.shown ? <p>✅ On the Big Screen</p> : <button className="main" onClick={() => f.announcements.approve(a.id)}>➤ {t.show}</button>}
        <button onClick={() => print()}>🖨 {t.print}</button>
        <h3>Tell in person: {reach.remaining} left</h3>
        <ul className="list">{reach.list.map(r => (
          <li key={r.id}><label><input type="checkbox" checked={r.told} onChange={() => f.announcements.toggleTold(a.id, r.id)} /> <b>{name(r.id)}</b> · {r.reasons.join(', ')}</label></li>
        ))}</ul>
      </div>}
    </section>
  );
}

function Field() {
  const t = useT(), f = useF();
  const list = f.views.field();
  const [note, setNote] = useState('');
  const [r, setR] = useState<RouteResult | null>(null);
  const [pos, setPos] = useState<{ lat: number; lng: number } | null>(null);
  const report = async (cloud = false) => { setR(await f.field.report(note, pos, cloud)); };
  return (
    <section>
      <p className="muted">Hazard: {f.rules.currentEvent} · map data: demo region pack (synthetic). Never “safe”: only “higher”.</p>
      {list.length === 0 && <Empty>{t.allChecked}</Empty>}
      {list.map(row => (
        <div key={row.id} className={`card ${row.tier === 'P1' ? 'danger' : ''}`}>
          <b>{row.tier} · {row.name}</b>{row.distanceM != null && <> · {row.distanceM} m</>} {row.needsReview && <Badge draft />}
          <div>Why: {row.why.join(' · ') || '—'}</div>
          {row.higher && <div>⬆️ Higher place: {row.higher.name}, {row.higher.distanceM} m {row.higher.direction}, +{row.higher.handGain} m</div>}
          <div className="muted">Status: {row.status}</div>
          <button onClick={() => f.field.visited(row.id, 'safe_home')}>✓ Safe at home</button>
          <button onClick={() => f.field.visited(row.id, 'needs_evacuation')}>🚨 Needs evacuation</button>
          <button onClick={() => { const reason = (document.getElementById(`ov-${row.id}`) as HTMLInputElement)?.value || 'staff decision'; f.field.override(row.id, row.tier === 'P1' ? 'P3' : 'P1', reason); }}>↕ Override</button>
          <input id={`ov-${row.id}`} placeholder="reason" />
        </div>
      ))}
      <RouteNotice r={r} onCloud={() => report(true)} />
      <textarea rows={2} value={note} placeholder="Pak Hasan, 78, can't walk, water knee-high, needs insulin" onChange={e => setNote(e.target.value)} />
      <button onClick={() => navigator.geolocation?.getCurrentPosition(p => setPos({ lat: p.coords.latitude, lng: p.coords.longitude }))}>📍 GPS {pos ? '✓' : ''}</button>
      <button className="main" disabled={!note.trim()} onClick={() => report()}>📝 {t.report}</button>
    </section>
  );
}

function Settings() {
  const s = useAppState(), f = useF();
  const [status, setStatus] = useState('');
  const [chunks, setChunks] = useState<string[] | null>(null);
  const [paste, setPaste] = useState('');
  const base = location.href.split('#')[0];
  return (
    <section>
      <div className="card"><h3>Shelter-ready</h3>
        <button className="main" onClick={async () => setStatus(await (f.prepareDevice?.() ?? f.router.run('announcement', { text: 'test' }).then(r => r.status)))}>⬇ Prepare on-device AI</button>
        <p>{status && `On-device model: ${status}`}</p></div>
      <div className="card"><h3>Language</h3>
        <button onClick={() => f.settings.setLang('en')} aria-pressed={s.lang === 'en'}>EN</button>
        <button onClick={() => f.settings.setLang('id')} aria-pressed={s.lang === 'id'}>ID</button></div>
      <div className="card"><label><input type="checkbox" checked={s.privateMode} onChange={e => f.settings.setPrivate(e.target.checked)} /> 🔒 Private mode: nothing leaves this device</label></div>
      <div className="card"><h3>Signal Scout</h3>
        <p>Signal: {s.signal} · last seen: {s.lastSignalAt ? new Date(s.lastSignalAt).toLocaleTimeString() : 'never'}</p>
        <button onClick={() => f.scout.check()}>📶 Check now</button>
        <button onClick={() => f.queue.flush()}>⇪ Sync now</button>
        <ul className="list">{s.jobs.slice(-8).map(j => <li key={j.id}>{j.type} · {j.state}{j.retries ? ` (retry ${j.retries})` : ''}</li>)}</ul></div>
      <div className="card"><h3>QR courier</h3>
        <button onClick={() => setChunks(f.queue.exportChunks())}>Export my queue as QR</button>
        {chunks?.map((c, i) => <div key={i}><QR text={c} label={`Courier ${i + 1}/${chunks.length}`} /> {i + 1}/{chunks.length}</div>)}
        <textarea rows={2} placeholder="Scout: paste scanned codes, one per line" value={paste} onChange={e => setPaste(e.target.value)} />
        <button onClick={() => { const n = f.queue.importChunks(paste.split('\n').filter(Boolean)); setStatus(`Imported ${n} job(s)`); setPaste(''); }}>Import</button></div>
      <div className="card"><h3>Join QRs (no login)</h3>
        <div className="row"><div><QR text={`${base}#/staff/today?join=${s.shelter.id}&role=volunteer`} label="Join as volunteer" size={140} /><br />Volunteer</div>
          <div><QR text={`${base}#/staff/today?join=${s.shelter.id}&role=medic`} label="Join as medic" size={140} /><br />Medic</div>
          <div><QR text={`${base}#/public/news`} label="Shelter QR for evacuees" size={140} /><br />Evacuees</div></div>
        <p><a href="#/big">Open Big Screen</a> · <a href="#/public/news">Open Public pages</a></p></div>
    </section>
  );
}
