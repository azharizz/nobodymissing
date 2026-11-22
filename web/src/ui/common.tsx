import { createContext, useContext, useRef, useState, useSyncExternalStore, type CSSProperties, type ReactNode } from 'react';
import QRCode from 'qrcode';
import jsQR from 'jsqr';
import iconSet from '../../../shared/icons/icons.json';
import kit from '../../../shared/mascot/kit.json';
import type { Features } from '../features/featureFactory';
import type { RouteResult } from '../features/inferenceRouter';
import { regionPack as pack } from '../features/regionData';
import type { ImageInput } from '../ports';
import { dict } from './i18n';

export const FeaturesCtx = createContext<Features>(null!);
export const useF = () => useContext(FeaturesCtx);
export function useAppState() { const f = useF(); return useSyncExternalStore(f.store.subscribe, f.store.get); }
export function useT() { return dict[useAppState().lang]; }
export type T = ReturnType<typeof useT>;

/** Hash navigation. Screen-enter motion is CSS on a keyed wrapper; reduced motion is respected in CSS. */
export const navigate = (hash: string) => { location.hash = hash; };

/** Line icons from shared/icons (same paths on Android). */
export function Icon({ name, size = 22, label, className, sw = 2 }: { name: string; size?: number; label?: string; className?: string; sw?: number }) {
  const d = (iconSet.icons as Record<string, string>)[name] ?? iconSet.icons.info;
  return (
    <svg className={`icon ${className ?? ''}`} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={sw}
      strokeLinecap="round" strokeLinejoin="round" role={label ? 'img' : undefined} aria-label={label} aria-hidden={label ? undefined : true}>
      <path d={d} />
    </svg>
  );
}

/** Kit, the British Shorthair firefighter (shared/mascot/kit.json). Mood drives the motion:
 *  idle = breathing + blink + tail · device = amber lantern glowing · cloud = blue lantern, walking · think = head tilt, quick glow ·
 *  alert = lantern flashing red/blue, hop · sleep = eyes closed, lantern off. */
export type Mood = 'idle' | 'device' | 'cloud' | 'alert' | 'sleep' | 'think';
export function Kit({ mood = 'idle', size = 56, label = 'Kit' }: { mood?: Mood; size?: number; label?: string }) {
  const [w, h] = kit.viewBox;
  const c = kit.centers as Record<string, number[]>;
  const origin = (g: string) => (c[g] ? { transformOrigin: `${c[g][0]}px ${c[g][1]}px` } : undefined);
  return (
    <svg className={`kit kit-${mood}`} width={size} height={(size * h) / w} viewBox={`0 0 ${w} ${h}`} role="img" aria-label={label}>
      <g className="kit-cat">
        {kit.order.map(g => (
          <g key={g} className={`kit-${g}`} style={origin(g) as CSSProperties}>
            {kit.parts.filter(p => p.g === g).map((p, i) => (
              <path key={i} d={p.d} fill={p.fill ?? 'none'} stroke={p.stroke} strokeWidth={p.sw} strokeLinecap="round" strokeLinejoin="round" opacity={p.opacity} />
            ))}
          </g>
        ))}
      </g>
    </svg>
  );
}

/** Where the AI ran, always visible (icon + words, never colour alone). */
export function Badge({ source, draft, text }: { source?: 'device' | 'cloud' | 'keywords' | 'staff' | 'maps' | null; draft?: boolean; text?: string }) {
  const t = useT();
  if (draft) return <span className="badge review"><Icon name="alert" size={15} />{text ?? t.review}</span>;
  if (source === 'device') return <span className="badge local"><Icon name="device" size={15} />{text ?? t.madeOnPhone}</span>;
  if (source === 'cloud') return <span className="badge cloud"><Icon name="cloud" size={15} />{text ?? t.madeInCloud}</span>;
  if (source === 'maps') return <span className="badge maps"><Icon name="map" size={15} />{text ?? t.fromMaps}</span>;
  if (source === 'keywords') return <span className="badge kw"><Icon name="keywords" size={15} />{text ?? t.keywords}</span>;
  return null;
}

/** Never send private data to the cloud silently: the staff member decides here. */
export function RouteNotice({ r, onCloud }: { r: RouteResult | null; onCloud?: () => void }) {
  const t = useT();
  if (!r || r.status === 'ok') return null;
  if (r.status === 'ask_staff') return (
    <div className="notice warn rise" role="alert"><Icon name="cloud" /><div><p>{t.askCloud}</p>
      <button className="btn primary small" onClick={onCloud}><Icon name="send" />{t.sendCloud}</button></div></div>
  );
  if (r.status === 'draft') return <div className="notice warn rise"><Icon name="alert" /><p>{t.review}</p></div>;
  if (r.status === 'queued') return <div className="notice rise"><Icon name="clock" /><p>{t.queued}</p></div>;
  if (r.status === 'blocked') return <div className="notice rise"><Icon name="lock" /><p>{t.blocked}</p></div>;
  return <div className="notice warn rise" role="alert"><Icon name="alert" /><p>{t.invalid}</p></div>;
}

export async function fileToImage(file: Blob): Promise<{ image: ImageInput; url: string }> {
  const bmp = await createImageBitmap(file);
  return canvasImage(bmp, bmp.width, bmp.height);
}
/** Downscale to 1280 px (small prompt, fast on-device) and JPEG-encode. */
export function canvasImage(src: CanvasImageSource, w: number, h: number) {
  const scale = Math.min(1, 1280 / Math.max(w, h));
  const c = document.createElement('canvas');
  c.width = Math.round(w * scale); c.height = Math.round(h * scale);
  c.getContext('2d')!.drawImage(src, 0, 0, c.width, c.height);
  const url = c.toDataURL('image/jpeg', 0.85);
  return { image: { mimeType: 'image/jpeg' as const, base64: url.split(',')[1] }, url };
}

export function PhotoPick({ label, onPhoto, className = 'btn', children }: { label: string; onPhoto: (img: ImageInput, url: string) => void; className?: string; children?: ReactNode }) {
  return (
    <label className={className} aria-label={label}>{children ?? <><Icon name="camera" />{label}</>}
      <input type="file" accept="image/jpeg,image/png" capture="environment" hidden
        onChange={async e => { const file = e.target.files?.[0]; e.target.value = ''; if (file) { const { image, url } = await fileToImage(file); onPhoto(image, url); } }} />
    </label>
  );
}

const qrCache = new Map<string, string>();
const qrPending = new Set<string>();
export function QR({ text, size = 180, label }: { text: string; size?: number; label: string }) {
  const [, rerender] = useState(0);
  const url = qrCache.get(text);
  if (!url && !qrPending.has(text)) {
    qrPending.add(text);
    QRCode.toDataURL(text, { width: size, margin: 1, color: { dark: '#14161A', light: '#FFFFFF' } }).then(u => { qrCache.set(text, u); rerender(n => n + 1); }).catch(() => {});
  }
  return url ? <img className="qr" src={url} width={size} height={size} alt={label} /> : <div className="qr" style={{ width: size, height: size, background: '#fff' }} aria-label={label} />;
}

export const Empty = ({ children, mood = 'sleep' }: { children: ReactNode; mood?: Mood }) => <div className="empty rise"><Kit mood={mood} size={110} /><p>{children}</p></div>;

/** Card content icon (food, water, ...) from the announcement schema. */
export const cardIcon = (icon: string) => (icon in iconSet.icons ? icon : 'info');

export function greeting(t: T) {
  const h = new Date().getHours();
  return h < 11 ? t.goodMorning : h < 17 ? t.goodAfternoon : t.goodEvening;
}

/** OS text-to-speech (an output device, not an AI call). One utterance at a time. */
let speakingId = '';
const speakListeners = new Set<() => void>();
const setSpeaking = (id: string) => { speakingId = id; speakListeners.forEach(l => l()); };
export function useSpeaker() {
  const now = useSyncExternalStore(l => { speakListeners.add(l); return () => { speakListeners.delete(l); }; }, () => speakingId);
  const lang = useAppState().lang;
  return {
    speaking: (id: string) => now === id,
    toggle(id: string, text: string) {
      if (!('speechSynthesis' in window)) return;
      speechSynthesis.cancel();
      if (now === id) return setSpeaking('');
      const u = new SpeechSynthesisUtterance(text);
      u.lang = lang === 'id' ? 'id-ID' : 'en-GB'; u.rate = 0.9;
      u.onend = u.onerror = () => setSpeaking('');
      setSpeaking(id); speechSynthesis.speak(u);
    },
  };
}
export function SayButton({ id, text, className = '' }: { id: string; text: string; className?: string }) {
  const t = useT(), sp = useSpeaker(), on = sp.speaking(id);
  return <button className={`say ${className} ${on ? 'on' : ''}`} onClick={() => sp.toggle(id, text)}><Icon name={on ? 'stop' : 'speaker'} />{on ? t.stop : t.readAloud}</button>;
}

/** OS speech-to-text (Web Speech, on-device when Chrome offers it). The text then goes to Nano like typed text. */
export function useDictation(onText: (text: string) => void) {
  const lang = useAppState().lang;
  const [on, setOn] = useState(false);
  const rec = useRef<any>(null);
  const SR = (window as any).SpeechRecognition ?? (window as any).webkitSpeechRecognition;
  const start = (local = true) => {
    if (!SR) return;
    const r = new SR();
    r.lang = lang === 'id' ? 'id-ID' : 'en-US'; r.interimResults = true; r.continuous = false;
    if (local && 'processLocally' in r) r.processLocally = true;
    r.onresult = (e: any) => onText(Array.from(e.results as ArrayLike<any>).map(x => x[0].transcript).join(' '));
    r.onerror = (e: any) => { if (local && e.error === 'language-not-supported') start(false); else setOn(false); };
    r.onend = () => setOn(false);
    rec.current = r; r.start(); setOn(true);
  };
  return { supported: !!SR, on, toggle: () => (on ? rec.current?.stop() : start()) };
}
export function MicButton({ onText, label }: { onText: (t: string) => void; label?: string }) {
  const t = useT(), d = useDictation(onText);
  if (!d.supported) return null;
  return (
    <button className="mic" onClick={d.toggle} aria-pressed={d.on}>
      <Icon name={d.on ? 'stop' : 'mic'} />{d.on ? t.listening : (label ?? t.speak)}
      {d.on && <span className="wave" aria-hidden>{[0, 1, 2, 3, 4].map(i => <i key={i} style={{ '--w': i } as CSSProperties} />)}</span>}
    </button>
  );
}

/** Live QR scanner (camera via a ref callback, no useEffect). Calls onText for each decoded code until it returns true. */
export function QrScanner({ onText }: { onText: (text: string) => boolean }) {
  const t = useT();
  const [noCam, setNoCam] = useState(false);
  const attach = (v: HTMLVideoElement | null) => {
    if (!v) return;
    let stream: MediaStream | null = null, stop = false, last = '';
    const c = document.createElement('canvas'), ctx = c.getContext('2d', { willReadFrequently: true })!;
    const loop = () => {
      if (stop) return;
      if (v.videoWidth) {
        c.width = v.videoWidth; c.height = v.videoHeight; ctx.drawImage(v, 0, 0);
        const code = jsQR(ctx.getImageData(0, 0, c.width, c.height).data, c.width, c.height);
        if (code?.data && code.data !== last) { last = code.data; if (onText(code.data)) return; }
      }
      requestAnimationFrame(loop);
    };
    navigator.mediaDevices?.getUserMedia({ video: { facingMode: 'environment' }, audio: false })
      .then(st => { if (stop) return st.getTracks().forEach(x => x.stop()); stream = st; v.srcObject = st; v.play().catch(() => {}); loop(); })
      .catch(() => setNoCam(true));
    return () => { stop = true; stream?.getTracks().forEach(x => x.stop()); };
  };
  return <div className="qr-scan">{noCam ? <p className="note"><Icon name="camera" size={16} />{t.noCamera}</p> : <><video ref={attach} playsInline muted /><span className="qr-frame" /><span className="glass">{t.scanHint}</span></>}</div>;
}

// ---------- Map: OpenStreetMap tiles (cached by the service worker for offline) + our hazard overlay + markers ----------
export type Marker = { id: string; lat: number; lng: number; icon: string; color: string; label: string; halo?: boolean; sel?: boolean; onClick?: () => void };
const TILE = 256;
/** Web Mercator world pixels at zoom z (same projection as the tile servers). */
export function worldPx(lat: number, lng: number, z: number) {
  const n = TILE * 2 ** z, r = (lat * Math.PI) / 180;
  return [((lng + 180) / 360) * n, ((1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2) * n];
}
/** Fit the points into a W x H view: highest zoom where they use at most 60% of the box. */
export function fitView(pts: { lat: number; lng: number }[], W: number, H: number, maxZoom: number) {
  let z = maxZoom;
  for (; z > 3; z--) {
    const px = pts.map(p => worldPx(p.lat, p.lng, z));
    const w = Math.max(...px.map(p => p[0])) - Math.min(...px.map(p => p[0])), h = Math.max(...px.map(p => p[1])) - Math.min(...px.map(p => p[1]));
    if (w <= W * 0.6 && h <= H * 0.6) break;
  }
  const px = pts.map(p => worldPx(p.lat, p.lng, z));
  const cx = (Math.max(...px.map(p => p[0])) + Math.min(...px.map(p => p[0]))) / 2, cy = (Math.max(...px.map(p => p[1])) + Math.min(...px.map(p => p[1]))) / 2 + H * 0.04;
  return { z, left: cx - W / 2, top: cy - H / 2 };
}

export function ShelterMap({ markers, you, chips, aspect = 4 / 5, className = '' }: { markers: Marker[]; you?: { lat: number; lng: number } | null; chips?: ReactNode; aspect?: number; className?: string }) {
  const f = useF(), t = useT();
  const g = pack.grid, cfg = f.rules.map;
  const W = 400, H = W / aspect;
  const pts = [...markers, ...(you ? [you] : [])];
  const { z, left, top } = fitView(pts.length ? pts : [{ lat: g.origin[1], lng: g.origin[0] }, { lat: g.origin[1] + g.rows * g.step, lng: g.origin[0] + g.cols * g.step }], W, H, cfg.maxZoom);
  const xy = (lat: number, lng: number) => { const [x, y] = worldPx(lat, lng, z); return [x - left, y - top]; };
  const tiles: { x: number; y: number; url: string }[] = [];
  for (let tx = Math.floor(left / TILE); tx * TILE < left + W; tx++) for (let ty = Math.floor(top / TILE); ty * TILE < top + H; ty++)
    tiles.push({ x: tx * TILE - left, y: ty * TILE - top, url: cfg.tiles.replace('{z}', String(z)).replace('{x}', String(tx)).replace('{y}', String(ty)) });
  const pct = (p: { lat: number; lng: number }) => { const [x, y] = xy(p.lat, p.lng); return { left: `${(x / W) * 100}%`, top: `${(y / H) * 100}%` }; };
  return (
    <div className={`map ${className}`} style={{ aspectRatio: String(aspect) }}>
      <svg className="base" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" aria-hidden>
        <defs><pattern id="hatch" width="8" height="8" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="8" height="8" fill="#5E95CF" opacity=".18" /><rect width="3" height="8" fill="#2C5A8C" opacity=".35" /></pattern></defs>
        <rect width={W} height={H} fill="#EEEDE6" />
        {tiles.map(tl => <image key={tl.url} href={tl.url} x={tl.x} y={tl.y} width={TILE} height={TILE} style={{ filter: 'saturate(.55) contrast(.95)' }} />)}
        {g.hand.map((row, r) => row.map((hand, c) => {
          if (hand > 5) return null;
          const [x0, y1] = xy(g.origin[1] + r * g.step, g.origin[0] + c * g.step), [x1, y0] = xy(g.origin[1] + (r + 1) * g.step, g.origin[0] + (c + 1) * g.step);
          return <rect key={`${r}-${c}`} x={x0} y={y0} width={x1 - x0} height={y1 - y0} fill={hand < 2 ? 'rgba(44,90,140,.28)' : 'url(#hatch)'} />;
        }))}
      </svg>
      {chips && <div className="chips">{chips}</div>}
      {you && <span className="you" style={pct(you)} aria-label={t.you} />}
      {markers.map(m => (
        <button key={m.id} className={`marker ${m.halo ? 'halo' : ''} ${m.sel ? 'sel' : ''}`} style={pct(m)} onClick={m.onClick} aria-label={m.label} tabIndex={m.onClick ? 0 : -1}>
          <span className="m" style={{ background: m.color }}><Icon name={m.icon} size={20} /></span><span className="t">{m.label}</span>
        </button>
      ))}
      <a className="map-credit" href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">{t.mapCredit}</a>
    </div>
  );
}
