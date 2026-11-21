import { useState, type CSSProperties } from 'react';
import { drugList as drugs } from '../features/regionData';
import type { RouteResult } from '../features/inferenceRouter';
import type { ImageInput } from '../ports';
import { fmt } from './i18n';
import { Badge, cardIcon, Empty, Icon, Kit, PhotoButton, QR, RouteNotice, StatusBar, Tile, useAppState, useF, useT } from './common';

type Tab = 'today' | 'people' | 'medicine' | 'announce' | 'field' | 'settings';
const TABS: [Tab, string][] = [['today', 'home'], ['people', 'people'], ['medicine', 'medicine'], ['announce', 'announce'], ['field', 'field'], ['settings', 'settings']];

export function Staff({ tab, go }: { tab: Tab; go: (t: Tab) => void }) {
  const t = useT(), s = useAppState(), f = useF();
  const idx = TABS.findIndex(([k]) => k === tab);
  const c = f.views.counts();
  return (
    <div className="app">
      <StatusBar title={s.shelter.name} />
      {s.scoutAlert && <ScoutAlert />}
      <main key={tab} className="view">
        <h1 className="view-title">{t[tab]}</h1>
        {tab === 'today' && <Today go={go} />}
        {tab === 'people' && <People />}
        {tab === 'medicine' && <Medicine />}
        {tab === 'announce' && <Announce />}
        {tab === 'field' && <Field />}
        {tab === 'settings' && <Settings />}
      </main>
      <nav className="tabs" style={{ '--i': idx, '--n': TABS.length } as CSSProperties}>
        <span className="tab-indicator" aria-hidden />
        {TABS.map(([k, icon]) => (
          <button key={k} aria-current={tab === k} aria-label={t[k]} onClick={() => go(k)}>
            <Icon name={icon} size={22} /><span>{k === 'settings' ? '' : t[k]}</span>
          </button>
        ))}
      </nav>
      <footer className="counter">
        <span><Icon name="device" size={13} />{c.device}</span><span><Icon name="cloud" size={13} />{c.cloud}</span>
        <span><Icon name="clock" size={13} />{s.jobs.filter(j => j.state !== 'sent').length}</span>
        <span className="sr-only">{fmt(t.counter, { device: c.device, cloud: c.cloud, waiting: s.jobs.filter(j => j.state !== 'sent').length })}</span>
      </footer>
    </div>
  );
}

function ScoutAlert() {
  const t = useT(), f = useF(), p = f.queue.progress();
  return (
    <div className="scout-alert" role="alertdialog" aria-label={t.signalBack}>
      <div className="ripples" aria-hidden><i /><i /><i /></div>
      <div className="scout-truck"><Kit mood="alert" size={280} /></div>
      <h1>{t.signalBack}</h1>
      <p>{fmt(t.sentOf, p)}</p>
      <button className="btn dark" onClick={f.scout.dismiss}><Icon name="check" />OK</button>
    </div>
  );
}

function Today({ go }: { go: (t: Tab) => void }) {
  const t = useT(), s = useAppState(), f = useF();
  const sch = f.views.schedule(), reach = f.views.reach(), field = f.views.field();
  const nextSlot = Object.keys(sch.counts)[0];
  const help = s.help.filter(h => !h.done);
  return (
    <>
      {s.signal === 'no_signal' && s.lastSignalAt == null && (
        <div className="hero rise"><Kit mood="device" size={110} /><div><b>{t.noSignal}</b><p>{t.airplaneTip}</p></div></div>
      )}
      <div className="grid2">
        <Tile i={0} icon="medicine" n={nextSlot ? sch.counts[nextSlot] : 0} label={`${t.nextRound} ${nextSlot ?? ''}`} onClick={() => go('medicine')} />
        <Tile i={1} icon="alert" n={sch.alerts.length} label={t.cantMiss} tone={sch.alerts.length ? 'danger' : undefined} onClick={() => go('medicine')} />
        <Tile i={2} icon="announce" n={reach.remaining} label={t.toTell} onClick={() => go('announce')} />
        <Tile i={3} icon="field" n={field.filter(r => r.tier === 'P1').length} label={t.p1Visits} tone={field.some(r => r.tier === 'P1') ? 'danger' : undefined} onClick={() => go('field')} />
        <Tile i={4} icon="help" n={help.length} label={t.help} tone={help.length ? 'accent' : undefined} onClick={() => go('people')} />
        <Tile i={5} icon="people" n={s.people.length} label={t.people} onClick={() => go('people')} />
      </div>
      {help.map((h, i) => (
        <div key={h.id} className="card row rise" style={{ '--d': i } as CSSProperties}>
          <Icon name="help" /><span className="grow"><b>{h.kind}</b> · <Icon name="pin" size={15} /> {h.spot}</span>
          <button className="btn icon-only" aria-label={t.confirm} onClick={() => f.settings.helpDone(h.id)}><Icon name="check" /></button>
        </div>
      ))}
      {s.people.length === 0 && <Empty>{t.emptyShelter}</Empty>}
    </>
  );
}

function People() {
  const t = useT(), s = useAppState(), f = useF();
  const [photo, setPhoto] = useState<{ img: ImageInput; url: string } | null>(null);
  const [r, setR] = useState<RouteResult | null>(null);
  const [q, setQ] = useState('');
  const [busy, setBusy] = useState(false);
  const scan = async (img: ImageInput, url: string, cloud = false) => { setPhoto({ img, url }); setBusy(true); setR(await f.roster.scan(img, cloud)); setBusy(false); };
  const review = s.people.filter(p => !p.confirmedBy);
  const list = s.people.filter(p => p.confirmedBy && p.nameAsWritten.toLowerCase().includes(q.toLowerCase()));
  return (
    <section>
      {photo && <figure className={`source rise ${busy ? 'scanning' : ''}`}><img src={photo.url} alt="Original list photo" /><figcaption>{t.original}</figcaption></figure>}
      <RouteNotice r={r} onCloud={() => photo && scan(photo.img, photo.url, true)} />
      {s.batch.length > 0 && <div className="notice rise"><Icon name="clock" /><p>{fmt(t.photosWaiting, { n: s.batch.length })}</p><button className="btn" onClick={() => f.roster.processBatch()}>{t.processNow}</button></div>}
      {review.map((p, i) => (
        <div key={p.id} className={`card rise ${p.needsReview ? 'uncertain' : ''}`} style={{ '--d': i } as CSSProperties}>
          <Badge source={p.inferredBy === 'staff' ? undefined : p.inferredBy} draft={p.needsReview} />
          <PersonForm p={p} />
        </div>
      ))}
      <label className="search"><Icon name="search" /><input placeholder={t.search} value={q} onChange={e => setQ(e.target.value)} /></label>
      <ul className="list">
        {list.map((p, i) => (
          <li key={p.id} className="rise" style={{ '--d': Math.min(i, 8) } as CSSProperties}>
            <span className="avatar">{p.nameAsWritten.replace(/^(Pak|Ibu|Bu|Bapak)\s+/i, '').slice(0, 1)}</span>
            <span className="grow"><b>{p.nameAsWritten}</b><small>{p.age ?? '?'} · {p.flags.map(fl => fl.replace(/_/g, ' ')).join(', ')}</small></span>
            {(p.flags.includes('deaf') || p.flags.includes('hard_of_hearing')) && <span className="pill-tag">{t.announce}</span>}
          </li>
        ))}
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
    <div className="form">
      <div className="field-row">
        <label className="grow">Name <input value={name} onChange={e => setName(e.target.value)} /></label>
        <label className="narrow">Age <input inputMode="numeric" value={age} onChange={e => setAge(e.target.value)} /></label>
      </div>
      {p.notes && <p className="muted"><Icon name="edit" size={15} /> {p.notes}</p>}
      <div className="chips wrap">{FLAGS.map(fl => (
        <label key={fl} className={`chip toggle ${flags.includes(fl) ? 'on' : ''}`}>
          <input type="checkbox" checked={flags.includes(fl)} onChange={e => setFlags(e.target.checked ? [...flags, fl] : flags.filter(x => x !== fl))} />{fl.replace(/_/g, ' ')}
        </label>
      ))}</div>
      <button className="btn primary" onClick={() => f.roster.confirm(p.id, { nameAsWritten: name, age: age ? Number(age) : null, flags })}><Icon name="check" />{t.confirm}</button>
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
      {sch.alerts.map((a, i) => (
        <div key={a.personId + a.generic} className="card danger row rise" style={{ '--d': i } as CSSProperties}>
          <span className="pulse-dot" aria-hidden /><span className="grow"><small>{t.cantMiss}</small><b>{name(a.personId)}</b> · {a.generic}</span>
          <span className="supply"><b>{a.supplyDays ?? '?'}</b><small>{t.days}</small></span>
        </div>
      ))}
      {Object.keys(sch.slots).length === 0 && <Empty>{t.noMeds}</Empty>}
      {Object.entries(sch.slots).map(([slot, list], si) => (
        <div key={slot} className="card rise" style={{ '--d': si + sch.alerts.length } as CSSProperties}>
          <div className="slot-head"><span className="time">{slot}</span><span className="count">{list.length}</span></div>
          {list.map(e => {
            const st = s.round[f.medicine.key(slot, e.personId, e.generic)];
            return (
              <div key={e.personId + e.generic} className={`round-row ${st ?? ''}`}>
                <span className="grow"><b>{name(e.personId)}</b><small>{e.generic} · {e.doseAsWritten}</small></span>
                <span className="seg">
                  <button aria-pressed={st === 'taken'} aria-label={t.given} onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'taken')}><Icon name="check" size={18} /></button>
                  <button aria-pressed={st === 'refused'} aria-label={t.refused} onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'refused')}><Icon name="x" size={18} /></button>
                  <button aria-pressed={st === 'missing'} aria-label={t.missing} onClick={() => f.medicine.mark(slot, e.personId, e.generic, 'missing')}><Icon name="missing" size={18} /></button>
                </span>
              </div>
            );
          })}
        </div>
      ))}
      {sch.shortages.length > 0 && <div className="card rise"><h3><Icon name="package" />{t.stock}</h3>
        {sch.shortages.map(x => <p key={x.generic + x.strength}>{fmt(t.outOf, { n: x.people, drug: `${x.generic} ${x.strength ?? ''}`, days: f.rules.shortageDays })}</p>)}
        <button className="btn primary" onClick={f.medicine.requestSupply}><Icon name="send" />{t.requestSupplies}</button>
        <p className="muted">{t.countsOnly}</p></div>}
      {photo && <figure className="source rise"><img src={photo.url} alt="Original medicine photo" /><figcaption>{t.original}</figcaption></figure>}
      <RouteNotice r={r} onCloud={() => photo && read(photo.img, photo.url, true)} />
      {pending.map(({ c, m, i }) => (
        <div key={c.personId + i} className="card uncertain rise">
          <Badge source={m.inferredBy} draft />
          <p><b>{name(c.personId)}</b>: “{m.nameAsWritten}” {m.strength} · {m.doseAsWritten} · {m.timesPerDay ?? '?'}×/day</p>
          {m.match.lookAlikes.length > 0 && <p className="warn"><Icon name="alert" size={16} />{fmt(t.lookAlike, { names: m.match.lookAlikes.join(', ') })}</p>}
          {m.match.strengthKnown === false && <p className="warn"><Icon name="alert" size={16} />{t.strengthUnknown}</p>}
          {m.match.status === 'unknown' && <p className="warn"><Icon name="info" size={16} />{t.unknownName}</p>}
          <div className="chips wrap">
            {[...(m.match.generic ? [m.match.generic] : []), ...m.match.candidates].map(g => (
              <span key={g} className="confirm-pair">
                <button className="btn" onClick={() => f.careCard.confirmMed(c.personId, i, g, 'self')}><Icon name="check" size={16} />{g} · {t.evacuees}</button>
                <button className="btn" onClick={() => f.careCard.confirmMed(c.personId, i, g, 'medic')}><Icon name="doctor" size={16} />{t.medic}</button>
              </span>
            ))}
          </div>
        </div>
      ))}
      <p className="muted">{drugs._notice}</p>
      <label className="select">{t.person}<select value={pid} onChange={e => setPid(e.target.value)}>{s.people.map(p => <option key={p.id} value={p.id}>{p.nameAsWritten}</option>)}</select></label>
      <PhotoButton label={t.medicinePhoto} onPhoto={(img, url) => read(img, url)} />
    </section>
  );
}

export function AnnouncementCards({ a, big }: { a: ReturnType<typeof useAppState>['announcements'][number]; big?: boolean }) {
  // Keyword fallback (no AI): a short title, the parsed time and place; the full original is always shown beside it.
  const cards = a.cards ?? [{ icon: a.icons[0] ?? 'info', title: a.text.split(/\s+/).slice(0, 4).join(' '), time: a.times[0], place: a.place }];
  return (
    <div className={`kcards ${big ? 'big' : ''}`}>
      {cards.map((c, i) => (
        <div key={i} className="kcard rise" style={{ '--d': i } as CSSProperties}>
          <span className={`kcard-icon ic-${cardIcon(c.icon)}`}><Icon name={cardIcon(c.icon)} size={big ? 72 : 30} /></span>
          <span className="kcard-text"><b>{c.title}</b>{c.time && <span className="kcard-time"><Icon name="clock" size={big ? 40 : 16} />{c.time}</span>}{c.place && <span className="kcard-place"><Icon name="pin" size={big ? 40 : 16} />{c.place}</span>}</span>
        </div>
      ))}
    </div>
  );
}

function Announce() {
  const t = useT(), s = useAppState(), f = useF();
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const a = s.announcements[0];
  const reach = f.views.reach(a?.id);
  const name = (id: string) => s.people.find(p => p.id === id)?.nameAsWritten ?? id;
  return (
    <section>
      <div className="composer">
        <textarea rows={3} value={text} placeholder={t.announcementHint} onChange={e => setText(e.target.value)} />
        <button className="btn primary" disabled={!text.trim() || busy} onClick={async () => { setBusy(true); await f.announcements.create(text.trim()); setText(''); setBusy(false); }}>
          <Icon name="sparkle" className={busy ? 'spin' : ''} />{t.makeCards}
        </button>
      </div>
      {a && <div className="card rise" key={a.id}>
        <div className="card-head"><Badge source={a.source} draft={a.draft} /><span className="muted">{t.original}: “{a.text}”</span></div>
        <div className="preview"><Kit mood={a.source === 'cloud' ? 'cloud' : 'device'} size={110} /><AnnouncementCards a={a} /></div>
        <div className="actions">
          {a.shown ? <span className="done"><Icon name="check" />{t.shown}</span> : <button className="btn primary" onClick={() => f.announcements.approve(a.id)}><Icon name="send" />{t.show}</button>}
          <button className="btn" onClick={() => print()}><Icon name="print" />{t.print}</button>
        </div>
        <h3>{fmt(t.tellInPerson, { n: reach.remaining })}</h3>
        <div className="progress"><span style={{ width: `${reach.list.length ? (100 * (reach.list.length - reach.remaining)) / reach.list.length : 0}%` }} /></div>
        <ul className="list">{reach.list.map(r => (
          <li key={r.id} className={r.told ? 'told' : ''}><label className="check-row">
            <input type="checkbox" checked={r.told} onChange={() => f.announcements.toggleTold(a.id, r.id)} /><span className="box"><Icon name="check" size={16} /></span>
            <span className="grow"><b>{name(r.id)}</b><small>{r.reasons.map(x => x.replace(/_/g, ' ')).join(', ')}</small></span></label></li>
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
      <p className="muted"><Icon name="info" size={15} />{fmt(t.hazardNote, { event: f.rules.currentEvent })}</p>
      {list.length === 0 && <Empty>{t.allChecked}</Empty>}
      {list.map((row, i) => (
        <div key={row.id} className={`card visit rise tier-${row.tier}`} style={{ '--d': i } as CSSProperties}>
          <div className="visit-head"><span className="tier">{row.tier}</span><b className="grow">{row.name}</b>{row.distanceM != null && <span className="muted">{row.distanceM} m</span>}{row.needsReview && <Badge draft />}</div>
          <div className="why">{row.why.length ? row.why.map(w => <span key={w} className="pill-tag">{w}</span>) : <span className="muted">—</span>}</div>
          {row.higher && <p className="higher"><Icon name="higher" size={18} />{t.higherPlace}: <b>{row.higher.name}</b> · {row.higher.distanceM} m {row.higher.direction} · +{row.higher.handGain} m</p>}
          <div className="actions">
            <button className="btn" onClick={() => f.field.visited(row.id, 'safe_home')}><Icon name="check" />{t.safeAtHome}</button>
            <button className="btn danger" onClick={() => f.field.visited(row.id, 'needs_evacuation')}><Icon name="evacuate" />{t.needsEvacuation}</button>
            <button className="btn ghost" onClick={() => { const reason = (document.getElementById(`ov-${row.id}`) as HTMLInputElement)?.value || 'staff decision'; f.field.override(row.id, row.tier === 'P1' ? 'P3' : 'P1', reason); }}><Icon name="override" />{t.override}</button>
            <input id={`ov-${row.id}`} className="reason" placeholder={t.reason} />
          </div>
          <small className="muted">{row.status.replace(/_/g, ' ')}</small>
        </div>
      ))}
      <RouteNotice r={r} onCloud={() => report(true)} />
      <div className="composer">
        <textarea rows={2} value={note} placeholder={t.fieldHint} onChange={e => setNote(e.target.value)} />
        <button className={`btn ${pos ? 'on' : ''}`} onClick={() => navigator.geolocation?.getCurrentPosition(p => setPos({ lat: p.coords.latitude, lng: p.coords.longitude }))}><Icon name={pos ? 'check' : 'pin'} />GPS</button>
      </div>
      <button className="btn primary main-action" disabled={!note.trim()} onClick={() => report()}><Icon name="send" />{t.report}</button>
    </section>
  );
}

function Settings() {
  const s = useAppState(), f = useF(), t = useT();
  const [status, setStatus] = useState('');
  const [chunks, setChunks] = useState<string[] | null>(null);
  const [paste, setPaste] = useState('');
  const base = location.href.split('#')[0];
  return (
    <section className="settings">
      <div className="card rise" style={{ '--d': 0 } as CSSProperties}><h3><Icon name="download" />{t.shelterReady}</h3>
        <button className="btn primary" onClick={async () => setStatus(fmt(t.onDeviceModel, { status: await (f.prepareDevice?.() ?? Promise.resolve('demo')) }))}><Icon name="device" />{t.prepareDevice}</button>
        {status && <p className="muted">{status}</p>}</div>
      <div className="card rise" style={{ '--d': 1 } as CSSProperties}><h3><Icon name="globe" />{t.language}</h3>
        <div className="seg wide">
          <button aria-pressed={s.lang === 'en'} onClick={() => f.settings.setLang('en')}>English</button>
          <button aria-pressed={s.lang === 'id'} onClick={() => f.settings.setLang('id')}>Bahasa Indonesia</button></div></div>
      <div className="card rise" style={{ '--d': 2 } as CSSProperties}><label className="switch-row"><Icon name="lock" /><span className="grow">{t.privateMode}</span>
        <input type="checkbox" role="switch" checked={s.privateMode} onChange={e => f.settings.setPrivate(e.target.checked)} /><span className="switch" aria-hidden /></label></div>
      <div className="card rise" style={{ '--d': 3 } as CSSProperties}><h3><Icon name="signal" />{t.signalScout}</h3>
        <p className="muted">{t.signal}: <b>{s.signal.replace('_', ' ')}</b> · {t.lastSeen}: {s.lastSignalAt ? new Date(s.lastSignalAt).toLocaleTimeString() : t.never}</p>
        <div className="actions"><button className="btn" onClick={() => f.scout.check()}><Icon name="signal" />{t.checkNow}</button>
          <button className="btn" onClick={() => f.queue.flush()}><Icon name="sync" />{t.syncNow}</button></div>
        <ul className="list jobs">{s.jobs.slice(-8).map(j => <li key={j.id}><span className={`dot ${j.state}`} />{j.type.replace(/_/g, ' ')}<small>{j.state}{j.retries ? ` · retry ${j.retries}` : ''}</small></li>)}</ul></div>
      <div className="card rise" style={{ '--d': 4 } as CSSProperties}><h3><Icon name="qr" />{t.courier}</h3>
        <button className="btn" onClick={() => setChunks(f.queue.exportChunks())}><Icon name="qr" />{t.exportQueue}</button>
        <div className="qr-row">{chunks?.map((c, i) => <figure key={i}><QR text={c} label={`${t.courier} ${i + 1}/${chunks.length}`} /><figcaption>{i + 1}/{chunks.length}</figcaption></figure>)}</div>
        <textarea rows={2} placeholder={t.importCodes} value={paste} onChange={e => setPaste(e.target.value)} />
        <button className="btn" onClick={() => { const n = f.queue.importChunks(paste.split('\n').filter(Boolean)); setStatus(fmt(t.imported, { n })); setPaste(''); }}><Icon name="download" />{t.import}</button></div>
      <div className="card rise" style={{ '--d': 5 } as CSSProperties}><h3><Icon name="people" />{t.joinQrs}</h3>
        <div className="qr-row">
          <figure><QR text={`${base}#/staff/today?join=${s.shelter.id}&role=volunteer`} label={t.volunteer} size={140} /><figcaption>{t.volunteer}</figcaption></figure>
          <figure><QR text={`${base}#/staff/today?join=${s.shelter.id}&role=medic`} label={t.medic} size={140} /><figcaption>{t.medic}</figcaption></figure>
          <figure><QR text={`${base}#/public/news`} label={t.evacuees} size={140} /><figcaption>{t.evacuees}</figcaption></figure></div>
        <p className="links"><a href="#/big"><Icon name="announce" size={16} />Big Screen</a><a href="#/public/news"><Icon name="people" size={16} />{t.news}</a></p></div>
      <div className="card rise" style={{ '--d': 6 } as CSSProperties}><button className="btn ghost" onClick={f.settings.loadDemo}><Icon name="demo" />{t.loadDemo}</button></div>
    </section>
  );
}
