import { useSyncExternalStore } from 'react';
import { BigScreen, Public } from './public';
import { Staff } from './staff';

// Hash routing via useSyncExternalStore (no router lib, no useEffect).
const subscribe = (cb: () => void) => { addEventListener('hashchange', cb); return () => removeEventListener('hashchange', cb); };
const go = (path: string) => { location.hash = path; };

export function App() {
  const hash = useSyncExternalStore(subscribe, () => location.hash);
  const [, view, tab] = hash.replace(/\?.*/, '').split('/');
  if (view === 'big') return <BigScreen />;
  if (view === 'public') return <Public tab={(tab as any) || 'news'} go={t => go(`#/public/${t}`)} />;
  return <Staff tab={(tab as any) || 'today'} go={t => go(`#/staff/${t}`)} />;
}
