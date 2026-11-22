import { createContext, useContext, useRef, useState, useSyncExternalStore, type CSSProperties, type ReactNode } from 'react';
import QRCode from 'qrcode';
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

// ---------- Map: the offline region pack drawn as a calm map (no tiles, no network) ----------
export type Marker = { id: string; lat: number; lng: number; icon: string; color: string; label: string; halo?: boolean; sel?: boolean; onClick?: () => void };
export function ShelterMap({ markers, you, chips, aspect = 4 / 5, className = '' }: { markers: Marker[]; you?: { lat: number; lng: number } | null; chips?: ReactNode; aspect?: number; className?: string }) {
  const g = pack.grid;
  const pts = [...markers, ...(you ? [you] : [])];
  let [minLat, maxLat, minLng, maxLng] = pts.length
    ? [Math.min(...pts.map(p => p.lat)), Math.max(...pts.map(p => p.lat)), Math.min(...pts.map(p => p.lng)), Math.max(...pts.map(p => p.lng))]
    : [g.origin[1], g.origin[1] + g.rows * g.step, g.origin[0], g.origin[0] + g.cols * g.step];
  // Pad, keep a minimum span, then match the box's aspect ratio (roughly equal metres per pixel near the equator).
  let spanLat = Math.max(maxLat - minLat, 0.006) * 1.6, spanLng = Math.max(maxLng - minLng, 0.006) * 1.5;
  if (spanLng / spanLat > aspect) spanLat = spanLng / aspect; else spanLng = spanLat * aspect;
  const cLat = (minLat + maxLat) / 2 + spanLat * 0.04, cLng = (minLng + maxLng) / 2;
  [minLat, maxLat, minLng, maxLng] = [cLat - spanLat / 2, cLat + spanLat / 2, cLng - spanLng / 2, cLng + spanLng / 2];
  const W = 400, H = W / aspect;
  const x = (lng: number) => ((lng - minLng) / spanLng) * W, y = (lat: number) => ((maxLat - lat) / spanLat) * H;
  const pct = (p: { lat: number; lng: number }) => ({ left: `${(x(p.lng) / W) * 100}%`, top: `${(y(p.lat) / H) * 100}%` });
  return (
    <div className={`map ${className}`} style={{ aspectRatio: String(aspect) }}>
      <svg className="base" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" aria-hidden>
        <defs><pattern id="hatch" width="10" height="10" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="10" height="10" fill="#CFE0EF" /><rect width="4" height="10" fill="#B9D1E8" /></pattern></defs>
        <rect width={W} height={H} fill="#EEEDE6" />
        {g.hand.map((row, r) => row.map((hand, c) => {
          if (hand > 5) return null;
          const lat0 = g.origin[1] + r * g.step, lng0 = g.origin[0] + c * g.step;
          return <rect key={`${r}-${c}`} x={x(lng0)} y={y(lat0 + g.step)} width={x(lng0 + g.step) - x(lng0)} height={y(lat0) - y(lat0 + g.step)} rx={18} fill={hand < 2 ? '#A9C7E3' : 'url(#hatch)'} />;
        }))}
      </svg>
      {chips && <div className="chips">{chips}</div>}
      {you && <span className="you" style={pct(you)} aria-label="You" />}
      {markers.map(m => (
        <button key={m.id} className={`marker ${m.halo ? 'halo' : ''} ${m.sel ? 'sel' : ''}`} style={pct(m)} onClick={m.onClick} aria-label={m.label} tabIndex={m.onClick ? 0 : -1}>
          <span className="m" style={{ background: m.color }}><Icon name={m.icon} size={20} /></span><span className="t">{m.label}</span>
        </button>
      ))}
    </div>
  );
}

