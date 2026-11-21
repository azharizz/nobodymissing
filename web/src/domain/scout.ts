// Signal Scout: real checks only; "back" after N successes in a row; alert once per recovery (6.10).
export type SignalState = 'no_signal' | 'maybe' | 'back';

export function scoutStep(run: number, ok: boolean, needed: number) {
  const next = ok ? run + 1 : 0;
  const state: SignalState = next === 0 ? 'no_signal' : next >= needed ? 'back' : 'maybe';
  return { run: next, state, alert: next === needed };
}

export function scoutStates(checks: boolean[], needed: number) {
  let run = 0;
  const states: SignalState[] = [], alertAt: number[] = [];
  checks.forEach((ok, i) => {
    const s = scoutStep(run, ok, needed);
    run = s.run; states.push(s.state);
    if (s.alert) alertAt.push(i);
  });
  return { states, alertAt };
}
