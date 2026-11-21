// F11 Signal Scout: real checks, "signal is back" after N successes, alert once, then sync in priority order.
import { scoutStep } from '../domain/scout';
import type { Deps } from './deps';

export function createScout(d: Deps, flush: () => Promise<void>) {
  async function check() {
    const ok = await d.connectivity.check();
    const s = scoutStep(d.store.get().scoutRun, ok, d.rules.scout.successesNeeded);
    d.store.set(st => ({ ...st, scoutRun: s.run, signal: s.state, scoutAlert: st.scoutAlert || s.alert, lastSignalAt: ok ? d.clock.now() : st.lastSignalAt }));
    if (s.state === 'back') await flush();
    return s;
  }
  const dismiss = () => d.store.set(s => ({ ...s, scoutAlert: false }));
  return { check, dismiss };
}
