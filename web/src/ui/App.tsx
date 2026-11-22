import { useSyncExternalStore } from 'react';
import { BigScreen, Public } from './public';
import { Staff } from './staff';

// Hash routing via useSyncExternalStore (no router lib, no useEffect). #/public/... = evacuees, #/staff/... = staff, #/big = hall TV.
const subscribe = (cb: () => void) => { addEventListener('hashchange', cb); return () => removeEventListener('hashchange', cb); };

export function App() {
  const hash = useSyncExternalStore(subscribe, () => location.hash);
  const [, view, ...rest] = hash.replace(/\?.*/, '').split('/');
  if (view === 'big') return <BigScreen />;
  if (view === 'public') return <Public tab={rest[0] ?? ''} />;
  return <Staff path={rest} />;
}
