import { describe, expect, it } from 'vitest';
import rules from '../../../shared/rules/defaults.json';
import { FakeInference } from '../adapters/fakes';
import { createInferenceRouter } from './inferenceRouter';

const ROSTER_OK = '{"rows":[{"name":"Hasan Basri","age":82,"confidence":0.9}]}';
const make = (inf: FakeInference, ctx = { online: true, privateMode: false }) => createInferenceRouter(inf, rules as any, () => ctx);

describe('InferenceRouter', () => {
  it('privacy guard: sensitive task with no on-device API asks staff and sends nothing', async () => {
    const inf = new FakeInference('no_api');
    const r = await make(inf).run('roster_read', { text: 'x' });
    expect(r.status).toBe('ask_staff');
    expect(inf.calls).toHaveLength(0);
  });

  it('sensitive task runs ONLY_ON_DEVICE and returns validated data with a device badge', async () => {
    const inf = new FakeInference('available', () => ({ raw: ROSTER_OK, source: 'device' }));
    const r = await make(inf).run('roster_read', { text: 'x' });
    expect(inf.calls[0].mode).toBe('only_on_device');
    expect(r).toMatchObject({ status: 'ok', source: 'device', data: { rows: [{ name: 'Hasan Basri' }] } });
  });

  it('privacy guard post-check: a cloud answer to an on-device-only request is discarded', async () => {
    const inf = new FakeInference('available', () => ({ raw: ROSTER_OK, source: 'cloud' }));
    const r = await make(inf).run('roster_read', { text: 'x' });
    expect(r.status).toBe('blocked');
    expect(r.data).toBeUndefined();
  });

  it('invalid AI output never enters the domain', async () => {
    const inf = new FakeInference('available', () => ({ raw: '{"rows":[{"name":"Hasan"}]}', source: 'device' }));
    const r = await make(inf).run('roster_read', { text: 'x' });
    expect(r.status).toBe('invalid');
    expect(r.data).toBeUndefined();
  });

  it('staff-approved cloud for a sensitive task uses ONLY_IN_CLOUD', async () => {
    const inf = new FakeInference('no_api', () => ({ raw: ROSTER_OK, source: 'cloud' }));
    const r = await make(inf).run('roster_read', { text: 'x' }, { staffApprovedCloud: true });
    expect(inf.calls[0].mode).toBe('only_in_cloud');
    expect(inf.calls[0].model).toBe(rules.models.cloudAccurate);
    expect(r.status).toBe('ok');
  });

  it('offline + needs cloud -> queued, nothing sent', async () => {
    const inf = new FakeInference('no_api');
    const r = await make(inf, { online: false, privateMode: false }).run('supply_request', { text: 'x' });
    expect(r.status).toBe('queued');
    expect(inf.calls).toHaveLength(0);
  });

  it('measured-weak device task offline -> device draft (needs review)', async () => {
    const inf = new FakeInference('available', () => ({ raw: '{"cards":[{"icon":"food","title":"Food"}]}', source: 'device' }));
    const r = await make(inf, { online: false, privateMode: false }).run('announcement_id', { text: 'Makan jam 12' });
    expect(r.status).toBe('draft');
  });

  it('slow device on a non-sensitive task falls back to the cloud', async () => {
    const inf = new FakeInference('available', r => r.mode === 'prefer_on_device'
      ? new Promise(res => setTimeout(() => res({ raw: '{}', source: 'device' }), 200))
      : { raw: '{"cards":[{"icon":"water","title":"Water"}]}', source: 'cloud' });
    const router = createInferenceRouter(inf, { ...rules, timeouts: { deviceMs: 20, cloudMs: 1000 } } as any, () => ({ online: true, privateMode: false }));
    const r = await router.run('announcement', { text: 'Water at 3' });
    expect(r).toMatchObject({ status: 'ok', source: 'cloud' });
    expect(inf.calls.map(c => c.mode)).toEqual(['prefer_on_device', 'only_in_cloud']);
  });

  it('slow device on a sensitive task never falls back silently', async () => {
    const inf = new FakeInference('available', () => new Promise(res => setTimeout(() => res({ raw: ROSTER_OK, source: 'device' }), 200)));
    const router = createInferenceRouter(inf, { ...rules, timeouts: { deviceMs: 20, cloudMs: 1000 } } as any, () => ({ online: true, privateMode: false }));
    const r = await router.run('roster_read', { text: 'x' });
    expect(r.status).toBe('ask_staff');
    expect(inf.calls).toHaveLength(1);
  });

  it('counts device and cloud answers for the live hybrid counter', async () => {
    const inf = new FakeInference('available', () => ({ raw: ROSTER_OK, source: 'device' }));
    const router = make(inf);
    await router.run('roster_read', { text: 'x' });
    await router.run('roster_read', { text: 'y' });
    expect(router.counts()).toEqual({ device: 2, cloud: 0 });
  });
});
