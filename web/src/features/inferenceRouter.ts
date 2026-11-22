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
type Input = { text?: string; image?: ImageInput; vars?: Record<string, string>; location?: { lat: number; lng: number } };

class Timeout extends Error {}
const withTimeout = <T,>(p: Promise<T>, ms: number) =>
  Promise.race([p, new Promise<never>((_, rej) => setTimeout(() => rej(new Timeout()), ms))]);

export type InferenceRouter = ReturnType<typeof createInferenceRouter>;

/** InferenceRouterFactory: builds the router from the Remote Config policy table. */
export function createInferenceRouter(inference: InferencePort, rules: RouterRules, ctx: Ctx, prompts: Record<string, string> = pack.tasks) {
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
    const policy = rules.routerPolicy[task];
    const { online, privateMode } = ctx();
    const status = await inference.onDeviceStatus();
    const onDeviceAvailable = isOnDeviceReady(status !== 'no_api', status);
    const d = decide(task, { onDeviceAvailable, online, privateMode, staffApprovedCloud: !!opts.staffApprovedCloud }, rules.routerPolicy, rules.accuracyBarPoints);
    if (d.action === 'ask_staff') return { status: 'ask_staff' };
    if (d.action === 'queue') return { status: 'queued' };
    if (d.action === 'block' || !d.mode) return { status: 'blocked' };

    let res: InferenceResult;
    try {
      res = await call(task, policy, d.mode, input);
    } catch (e) {
      const deviceTried = d.mode === 'only_on_device' || d.mode === 'prefer_on_device';
      // Slow/failed device: non-sensitive work may go to the cloud; sensitive work must ask staff first.
      if (deviceTried && policy.sensitive) return { status: 'ask_staff', errors: [String(e)] };
      if (deviceTried && online && !privateMode) {
        try { res = await call(task, policy, 'only_in_cloud', input); } catch (e2) { return { status: 'error', errors: [String(e2)] }; }
      } else return { status: e instanceof Timeout ? 'queued' : 'error', errors: [String(e)] };
    }
    // Privacy guard post-check (SDK#10635): an on-device-only request must never be answered by the cloud.
    if (d.mode === 'only_on_device' && res.source !== 'device') return { status: 'blocked', errors: ['cloud answered an on-device-only request'] };
    counts[res.source]++;
    const v = parseAndValidate<T>(res.raw, SCHEMAS[policy.schema]);
    if (!v.valid) return { status: 'invalid', source: res.source, errors: v.errors };
    return { status: d.action === 'device_draft' ? 'draft' : 'ok', data: v.value, source: res.source, places: res.places };
  }

  return { run, counts: () => ({ ...counts }) };
}
