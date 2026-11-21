// InferenceRouter policy (6.3): our own routing on top of the SDK, which only switches on availability.
export type Mode = 'only_on_device' | 'prefer_on_device' | 'prefer_in_cloud' | 'only_in_cloud';
export type Action = 'device' | 'device_draft' | 'cloud' | 'ask_staff' | 'queue' | 'block';
export type Decision = { action: Action; mode: Mode | null };
export type RouterCtx = { onDeviceAvailable: boolean; online: boolean; privateMode: boolean; staffApprovedCloud: boolean };
export type TaskPolicy = { sensitive: boolean; cloudOnly: boolean; schema: string; model: string; deviceAccuracy?: number | null; cloudAccuracy?: number | null };

const d = (action: Action, mode: Mode | null = null): Decision => ({ action, mode });

export function decide(task: string, ctx: RouterCtx, policy: Record<string, TaskPolicy>, barPoints: number): Decision {
  const p = policy[task];
  if (!p) return d('block');
  const { onDeviceAvailable: dev, online, privateMode: priv, staffApprovedCloud: ok } = ctx;
  if (p.cloudOnly) return priv ? d('block') : online ? d('cloud', 'only_in_cloud') : d('queue');
  if (p.sensitive) {
    // Names, health, photos: device only. Never send silently (guards SDK#10635).
    if (dev) return d('device', 'only_on_device');
    if (priv) return d('block');
    if (!ok) return d('ask_staff');
    return online ? d('cloud', 'only_in_cloud') : d('queue');
  }
  const deviceWeak = p.deviceAccuracy != null && p.cloudAccuracy != null && p.cloudAccuracy - p.deviceAccuracy > barPoints;
  if (deviceWeak) {
    if (online && !priv) return d('cloud', 'prefer_in_cloud');
    if (dev) return d('device_draft', 'only_on_device');
    return priv ? d('block') : d('queue');
  }
  if (dev) return d('device', priv ? 'only_on_device' : 'prefer_on_device');
  if (priv) return d('block');
  return online ? d('cloud', 'only_in_cloud') : d('queue');
}

/** Privacy guard: on-device only when the browser API exists AND the model is ready. */
export const isOnDeviceReady = (apiPresent: boolean, availability: string) => apiPresent && availability === 'available';
