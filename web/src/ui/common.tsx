import { createContext, useContext, useState, useSyncExternalStore, type ReactNode } from 'react';
import QRCode from 'qrcode';
import type { Features } from '../features/featureFactory';
import type { RouteResult } from '../features/inferenceRouter';
import type { ImageInput } from '../ports';
import { dict } from './i18n';
import { Kit } from './Kit';

export const FeaturesCtx = createContext<Features>(null!);
export const useF = () => useContext(FeaturesCtx);
export function useAppState() { const f = useF(); return useSyncExternalStore(f.store.subscribe, f.store.get); }
export function useT() { return dict[useAppState().lang]; }

/** Status = icon + word (never colour alone). */
export function Badge({ source, draft }: { source?: 'device' | 'cloud' | 'keywords'; draft?: boolean }) {
  const t = useT();
  if (draft) return <span className="badge review">⚠️ {t.review}</span>;
  if (source === 'device') return <span className="badge device">🏮 {t.device}</span>;
  if (source === 'cloud') return <span className="badge cloud">☁️ {t.cloud}</span>;
  if (source === 'keywords') return <span className="badge kw">🔤 {t.keywords}</span>;
  return null;
}

export function StatusBar({ title }: { title: string }) {
  const s = useAppState(), t = useT(), f = useF();
  const waiting = s.jobs.filter(j => j.state !== 'sent').length;
  const back = s.signal === 'back';
  return (
    <header className="status">
      <Kit pose={back && waiting ? 'bag' : 'lantern'} size={40} />
      <strong>{title}</strong>
      <span className="pill">{back ? (waiting ? `🎒 ${t.cloud}` : `📶 ${t.cloud}`) : `🏮 ${t.device}`}</span>
      {waiting > 0 && <span className="pill">⏳ {t.waiting} ({waiting})</span>}
      {s.privateMode && <span className="pill">🔒 Private</span>}
      {f.env === 'demo' && <span className="pill">DEMO: canned AI, no Firebase</span>}
    </header>
  );
}

/** Never send private data to the cloud silently: the staff member decides here. */
export function RouteNotice({ r, onCloud }: { r: RouteResult | null; onCloud?: () => void }) {
  const t = useT();
  if (!r || r.status === 'ok') return null;
  if (r.status === 'ask_staff') return (
    <div className="notice warn" role="alert"><p>{t.askCloud}</p>
      <button className="main" onClick={onCloud}>☁️ {t.sendCloud}</button></div>
  );
  if (r.status === 'draft') return <div className="notice warn">⚠️ {t.review}</div>;
  if (r.status === 'queued') return <div className="notice">⏳ {t.queued}</div>;
  if (r.status === 'blocked') return <div className="notice">🔒 {t.blocked}</div>;
  return <div className="notice warn" role="alert">{t.invalid}</div>;
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
    <label className="main photo">📷 {label}
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
    QRCode.toDataURL(text, { width: size, margin: 1 }).then(u => { qrCache.set(text, u); rerender(n => n + 1); }).catch(() => {});
  }
  return url ? <img src={url} width={size} height={size} alt={label} /> : <div style={{ width: size, height: size }} aria-label={label} />;
}

export function Tile({ icon, n, label, onClick, danger }: { icon: string; n: number | string; label: string; onClick?: () => void; danger?: boolean }) {
  return <button className={`tile${danger ? ' danger' : ''}`} onClick={onClick}><span className="num">{icon} {n}</span><span className="lbl">{label}</span></button>;
}

export const Empty = ({ children }: { children: ReactNode }) => <div className="empty"><Kit pose="sit" size={56} /><p>{children}</p></div>;

export const ICON: Record<string, string> = { food: '🍙', water: '💧', medicine: '💊', doctor: '🩺', move: '🚌', warning: '⚠️', toilet: '🚻', info: 'ℹ️', baby: '🍼', power: '🔌' };
