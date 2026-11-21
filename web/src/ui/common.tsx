import { createContext, useContext, useState, useSyncExternalStore, type CSSProperties, type ReactNode } from 'react';
import QRCode from 'qrcode';
import iconSet from '../../../shared/icons/icons.json';
import kit from '../../../shared/mascot/kit.json';
import type { Features } from '../features/featureFactory';
import type { RouteResult } from '../features/inferenceRouter';
import type { ImageInput } from '../ports';
import { dict } from './i18n';

export const FeaturesCtx = createContext<Features>(null!);
export const useF = () => useContext(FeaturesCtx);
export function useAppState() { const f = useF(); return useSyncExternalStore(f.store.subscribe, f.store.get); }
export function useT() { return dict[useAppState().lang]; }

export type IconName = keyof typeof iconSet.icons;
/** Line icons from shared/icons (same paths on Android). */
export function Icon({ name, size = 22, label, className }: { name: IconName | string; size?: number; label?: string; className?: string }) {
  const d = (iconSet.icons as Record<string, string>)[name] ?? iconSet.icons.info;
  return (
    <svg className={`icon ${className ?? ''}`} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
      strokeLinecap="round" strokeLinejoin="round" role={label ? 'img' : undefined} aria-label={label} aria-hidden={label ? undefined : true}>
      <path d={d} />
    </svg>
  );
}

/** Kit, the shelter's fire truck (shared/mascot/kit.json). Mood drives the motion:
 *  idle = gentle bob + blink · device = amber beacon glowing · cloud = blue beacon, wheels rolling · alert = red/blue flashing, bounce · sleep = eyes closed. */
export type Mood = 'idle' | 'device' | 'cloud' | 'alert' | 'sleep';
export function Kit({ mood = 'idle', size = 56, label = 'Kit' }: { mood?: Mood; size?: number; label?: string }) {
  const [w, h] = kit.viewBox;
  const c = kit.centers as Record<string, number[]>;
  const groups = ['shadow', 'body', 'face', 'eyes', 'glow', 'beacon', 'wheelL', 'wheelR'];
  const origin = (g: string) => (c[g] ? { transformOrigin: `${c[g][0]}px ${c[g][1]}px` } : undefined);
  return (
    <svg className={`kit kit-${mood}`} width={size} height={(size * h) / w} viewBox={`0 0 ${w} ${h}`} role="img" aria-label={label}>
      <g className="kit-truck">
        {groups.map(g => (
          <g key={g} className={`kit-${g}`} style={origin(g) as CSSProperties}>
            {kit.parts.filter(p => p.g === g).map((p, i) => (
              <path key={i} d={p.d} fill={p.fill ?? 'none'} stroke={p.stroke} strokeWidth={p.sw} strokeLinecap="round" strokeLinejoin="round" opacity={p.opacity} />
            ))}
          </g>
        ))}
      </g>
      {mood === 'cloud' && <g className="kit-speed" stroke="#14213D" strokeWidth={2.5} strokeLinecap="round"><path d="M2 40h8M0 50h12M4 60h7" /></g>}
    </svg>
  );
}

/** Status = icon + word (never colour alone). */
export function Badge({ source, draft }: { source?: 'device' | 'cloud' | 'keywords' | 'staff'; draft?: boolean }) {
  const t = useT();
  if (draft) return <span className="badge review"><Icon name="alert" size={15} />{t.review}</span>;
  if (source === 'device') return <span className="badge device"><Icon name="device" size={15} />{t.device}</span>;
  if (source === 'cloud') return <span className="badge cloud"><Icon name="cloud" size={15} />{t.cloud}</span>;
  if (source === 'keywords') return <span className="badge kw"><Icon name="keywords" size={15} />{t.keywords}</span>;
  return null;
}

export function StatusBar({ title }: { title: string }) {
  const s = useAppState(), t = useT(), f = useF();
  const waiting = s.jobs.filter(j => j.state !== 'sent').length;
  const back = s.signal === 'back';
  return (
    <header className="status">
      <Kit mood={back ? (waiting ? 'cloud' : 'idle') : 'device'} size={58} />
      <div className="status-title"><strong>{title}</strong><span>{back ? t.syncing : t.noSignal}</span></div>
      <div className="chips">
        <span className={`chip ${back ? 'cloud' : 'device'}`}><Icon name={back ? 'signal' : 'device'} size={15} />{back ? t.cloud : t.device}</span>
        {waiting > 0 && <span className="chip"><Icon name="clock" size={15} />{t.waiting} {waiting}</span>}
        {s.privateMode && <span className="chip"><Icon name="lock" size={15} />Private</span>}
        {f.env === 'demo' && <span className="chip demo"><Icon name="demo" size={15} />{t.demoBadge}</span>}
      </div>
    </header>
  );
}

/** Never send private data to the cloud silently: the staff member decides here. */
export function RouteNotice({ r, onCloud }: { r: RouteResult | null; onCloud?: () => void }) {
  const t = useT();
  if (!r || r.status === 'ok') return null;
  if (r.status === 'ask_staff') return (
    <div className="notice warn rise" role="alert"><Icon name="cloud" /><div><p>{t.askCloud}</p>
      <button className="btn primary" onClick={onCloud}><Icon name="send" />{t.sendCloud}</button></div></div>
  );
  if (r.status === 'draft') return <div className="notice warn rise"><Icon name="alert" /><p>{t.review}</p></div>;
  if (r.status === 'queued') return <div className="notice rise"><Icon name="clock" /><p>{t.queued}</p></div>;
  if (r.status === 'blocked') return <div className="notice rise"><Icon name="lock" /><p>{t.blocked}</p></div>;
  return <div className="notice warn rise" role="alert"><Icon name="alert" /><p>{t.invalid}</p></div>;
}

export async function fileToImage(file: File): Promise<{ image: ImageInput; url: string }> {
  const bmp = await createImageBitmap(file);
  const scale = Math.min(1, 1280 / Math.max(bmp.width, bmp.height));
  const c = document.createElement('canvas');
  c.width = Math.round(bmp.width * scale); c.height = Math.round(bmp.height * scale);
  c.getContext('2d')!.drawImage(bmp, 0, 0, c.width, c.height);
  const url = c.toDataURL('image/jpeg', 0.85);
  return { image: { mimeType: 'image/jpeg', base64: url.split(',')[1] }, url };
}

export function PhotoButton({ label, onPhoto }: { label: string; onPhoto: (img: ImageInput, url: string) => void }) {
  return (
    <label className="btn primary main-action"><Icon name="camera" />{label}
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
    QRCode.toDataURL(text, { width: size, margin: 1, color: { dark: '#14213D', light: '#FFFFFF' } }).then(u => { qrCache.set(text, u); rerender(n => n + 1); }).catch(() => {});
  }
  return url ? <img className="qr" src={url} width={size} height={size} alt={label} /> : <div className="qr skeleton" style={{ width: size, height: size }} aria-label={label} />;
}

export function Tile({ icon, n, label, onClick, tone, i = 0 }: { icon: string; n: number | string; label: string; onClick?: () => void; tone?: 'danger' | 'accent'; i?: number }) {
  return (
    <button className={`tile rise ${tone ?? ''}`} style={{ '--d': i } as CSSProperties} onClick={onClick}>
      <span className="tile-icon"><Icon name={icon} size={24} /></span>
      <span className="num">{n}</span>
      <span className="lbl">{label}</span>
    </button>
  );
}

export const Empty = ({ children }: { children: ReactNode }) => <div className="empty rise"><Kit mood="sleep" size={120} /><p>{children}</p></div>;

/** Card content icon (food, water, ...) from the announcement schema. */
export const cardIcon = (icon: string) => (icon in iconSet.icons ? icon : 'info');

/** Hash navigation. Screen-enter motion is CSS on a keyed wrapper (see App.tsx); reduced-motion is respected in CSS. */
export const navigate = (hash: string) => { location.hash = hash; };
