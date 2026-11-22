// InferenceRouter: the ONLY way features call AI (D11). Strategy = the policy table from Remote Config.
// Order: decide (domain/router) -> privacy guard -> Firebase AI Logic -> timeout fallback -> schema validation.
import pack from '../../../shared/prompts/pack.json';
import { decide, isOnDeviceReady, type Mode, type TaskPolicy } from '../domain/router';
import { parseAndValidate, SCHEMAS } from '../domain/validate';
import type { GroundingPlace, ImageInput, InferencePort, InferenceResult } from '../ports';

export type RouteStatus = 'ok' | 'draft' | 'ask_staff' | 'queued' | 'blocked' | 'invalid' | 'error';
export type RouteResult<T = any> = { status: RouteStatus; data?: T; source?: 'device' | 'cloud'; errors?: string[]; places?: GroundingPlace[] };
export type RouterRules = {
  routerPolicy: Record<string, TaskPolicy>; accuracyBarPoints: number; models: Record<string, string>;
  timeouts: { deviceMs: number; cloudMs: number };
};
type Ctx = () => { online: boolean; privateMode: boolean };
/** Why a call ran where it ran: shown to staff in "Where Kit ran". */
export type Why = 'private' | 'device_first' | 'needs_cloud' | 'measured' | 'device_slow' | 'staff_ok' | 'no_signal' | 'private_mode' | 'ask_staff' | 'cloud_fallback';
export type RunLog = { task: string; status: RouteStatus; source: 'device' | 'cloud' | null; why: Why; ms: number };
type Input = { text?: string; image?: ImageInput; vars?: Record<string, string>; location?: { lat: number; lng: number } };

class Timeout extends Error {}
const withTimeout = <T,>(p: Promise<T>, ms: number) =>
  Promise.race([p, new Promise<never>((_, rej) => setTimeout(() => rej(new Timeout()), ms))]);

export type InferenceRouter = ReturnType<typeof createInferenceRouter>;

/** InferenceRouterFactory: builds the router from the Remote Config policy table. */
export function createInferenceRouter(inference: InferencePort, rules: RouterRules, ctx: Ctx, prompts: Record<string, string> = pack.tasks, onAnswer?: (source: 'device' | 'cloud') => void, onLog?: (l: RunLog) => void) {
  const counts = { device: 0, cloud: 0 };

  async function call(task: string, policy: TaskPolicy, mode: Mode, input: Input): Promise<InferenceResult> {
    try { return await callModel(task, policy, mode, input, rules.models[policy.model]); }
    catch (e) {
      // Overloaded/failed primary cloud model -> backup model (6.14). Device modes don't use the model name.
      const backup = rules.models.cloudLite;
      if ((mode === 'only_in_cloud' || mode === 'prefer_in_cloud') && rules.models[policy.model] !== backup && !(e instanceof Timeout)) return callModel(task, policy, mode, input, backup);
      throw e;
    }
  }

  async function callModel(task: string, policy: TaskPolicy, mode: Mode, input: Input, model: string): Promise<InferenceResult> {
    let prompt = prompts[task] ?? '';
    for (const [k, v] of Object.entries(input.vars ?? {})) prompt = prompt.replace(`{${k}}`, v);
    const grounding = policy.grounding === 'maps' || policy.grounding === 'search' ? (policy.grounding as 'maps' | 'search') : undefined;
    const req = { task, prompt, text: input.text, image: input.image, schema: SCHEMAS[policy.schema], mode, model, grounding, location: input.location };
    const isDevice = mode === 'only_on_device' || mode === 'prefer_on_device';
    return withTimeout(inference.generate(req), isDevice ? rules.timeouts.deviceMs : rules.timeouts.cloudMs);
  }

  async function run<T = any>(task: string, input: Input, opts: { staffApprovedCloud?: boolean } = {}): Promise<RouteResult<T>> {
    const t0 = Date.now();
    const r = await route<T>(task, input, opts);
    onLog?.({ task, status: r.result.status, source: r.result.source ?? null, why: r.why, ms: Date.now() - t0 });
    return r.result;
  }

  async function route<T>(task: string, input: Input, opts: { staffApprovedCloud?: boolean }): Promise<{ result: RouteResult<T>; why: Why }> {
    const policy = rules.routerPolicy[task];
    const { online, privateMode } = ctx();
    const status = await inference.onDeviceStatus();
    const onDeviceAvailable = isOnDeviceReady(status !== 'no_api', status);
    const d = decide(task, { onDeviceAvailable, online, privateMode, staffApprovedCloud: !!opts.staffApprovedCloud }, rules.routerPolicy, rules.accuracyBarPoints);
    if (d.action === 'ask_staff') return { result: { status: 'ask_staff' }, why: 'ask_staff' };
    if (d.action === 'queue') return { result: { status: 'queued' }, why: 'no_signal' };
    if (d.action === 'block' || !d.mode) return { result: { status: 'blocked' }, why: 'private_mode' };
    // The reason, in the order the policy decided it.
    let why: Why = policy?.cloudOnly ? 'needs_cloud' : opts.staffApprovedCloud && d.action === 'cloud' ? 'staff_ok'
      : d.mode === 'prefer_in_cloud' ? 'measured' : policy?.sensitive ? 'private' : d.action === 'cloud' ? 'cloud_fallback' : 'device_first';

    let res: InferenceResult;
    try {
      res = await call(task, policy, d.mode, input);
    } catch (e) {
      const deviceTried = d.mode === 'only_on_device' || d.mode === 'prefer_on_device';
      // Slow/failed device: non-sensitive work may go to the cloud; sensitive work must ask staff first.
      if (deviceTried && policy.sensitive) return { result: { status: 'ask_staff', errors: [String(e)] }, why: 'ask_staff' };
      if (deviceTried && online && !privateMode) {
        why = 'device_slow';
        try { res = await call(task, policy, 'only_in_cloud', input); } catch (e2) { return { result: { status: 'error', errors: [String(e2)] }, why }; }
      } else return { result: { status: e instanceof Timeout ? 'queued' : 'error', errors: [String(e)] }, why: e instanceof Timeout ? 'no_signal' : why };
    }
    // Privacy guard post-check (SDK#10635): an on-device-only request must never be answered by the cloud.
    if (d.mode === 'only_on_device' && res.source !== 'device') return { result: { status: 'blocked', errors: ['cloud answered an on-device-only request'] }, why: 'private_mode' };
    // prefer_on_device answered by the cloud (SDK fallback): say so, never claim the device did it.
    if (why === 'device_first' && res.source === 'cloud') why = 'cloud_fallback';
    counts[res.source]++; onAnswer?.(res.source);
    const v = parseAndValidate<T>(res.raw, SCHEMAS[policy.schema]);
    if (!v.valid) return { result: { status: 'invalid', source: res.source, errors: v.errors }, why };
    return { result: { status: d.action === 'device_draft' ? 'draft' : 'ok', data: v.value, source: res.source, places: res.places }, why };
  }

  /** Load Gemini Nano into memory at app start so the first real question isn't slow (~15 s cold).
   *  Only when the model is already on the phone; on-device only, no user data, result ignored. */
  async function warm() {
    if ((await inference.onDeviceStatus()) !== 'available') return false;
    try { await withTimeout(inference.generate({ task: 'warm', prompt: 'Reply with {}', schema: { type: 'object' }, mode: 'only_on_device', model: rules.models.cloudLite }), rules.timeouts.deviceMs * 2); return true; }
    catch { return false; }
  }

  return { run, warm, counts: () => ({ ...counts }) };
}
