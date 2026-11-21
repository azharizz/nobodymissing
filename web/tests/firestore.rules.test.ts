// Firestore security rules (run inside the emulator: npm run test:rules).
import { readFileSync } from 'node:fs';
import { afterAll, beforeAll, describe, it } from 'vitest';
import { assertFails, assertSucceeds, initializeTestEnvironment, type RulesTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';

let env: RulesTestEnvironment;
beforeAll(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-nobody-missing', firestore: { rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 8080 } });
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(c => setDoc(doc(c.firestore(), 'shelters/SD3/members/staff1'), { role: 'volunteer' }));
});
afterAll(() => env?.cleanup());

const staff = () => env.authenticatedContext('staff1').firestore();
const stranger = () => env.authenticatedContext('someone').firestore();
const anon = () => env.unauthenticatedContext().firestore();

describe('firestore.rules', () => {
  it('a member writes the shelter roster', () => assertSucceeds(setDoc(doc(staff(), 'shelters/SD3/people/p1'), { nameAsWritten: 'Hasan' })));
  it('a non-member cannot write or read the roster', async () => {
    await assertFails(setDoc(doc(stranger(), 'shelters/SD3/people/p2'), { nameAsWritten: 'X' }));
    await assertFails(getDoc(doc(stranger(), 'shelters/SD3/people/p1')));
  });
  it('a member cannot add or promote other devices', () => assertFails(setDoc(doc(staff(), 'shelters/SD3/members/intruder'), { role: 'medic' })));
  it('a device can only join as itself with a known role', async () => {
    await assertSucceeds(setDoc(doc(stranger(), 'shelters/SD3/members/someone'), { role: 'volunteer', joinedAt: 1 }));
    await assertFails(setDoc(doc(stranger(), 'shelters/SD3/members/other'), { role: 'volunteer' }));
    await assertFails(setDoc(doc(env.authenticatedContext('z').firestore(), 'shelters/SD3/members/z'), { role: 'admin' }));
  });
  it('public family search: anyone reads, members write minimal fields only', async () => {
    await assertSucceeds(setDoc(doc(staff(), 'public/SD3_p1'), { name: 'Hasan', age: 80, shelterId: 'SD3', status: 'safe', hidden: false }));
    await assertSucceeds(getDoc(doc(anon(), 'public/SD3_p1')));
    await assertFails(setDoc(doc(staff(), 'public/SD3_p2'), { name: 'Hasan', age: 80, shelterId: 'SD3', status: 'safe', flags: ['deaf'] }));
    await assertFails(setDoc(doc(env.authenticatedContext('outsider').firestore(), 'public/SD3_p3'), { name: 'Fake', age: 30, shelterId: 'SD3', status: 'safe' }));
  });
  it("can't-miss alerts carry counts only, never names", async () => {
    await assertSucceeds(setDoc(doc(staff(), 'alerts/a1'), { shelterId: 'SD3', counts: { insulin: 1 }, minSupplyDays: 1 }));
    await assertFails(setDoc(doc(staff(), 'alerts/a2'), { shelterId: 'SD3', counts: { insulin: 1 }, names: ['Hasan'] }));
  });
  it('supply requests reject a names field', async () => {
    await assertSucceeds(setDoc(doc(staff(), 'requests/r1'), { items: [{ generic: 'insulin glargine', qty: 1 }] }));
    await assertFails(setDoc(doc(staff(), 'requests/r2'), { items: [], names: ['Hasan'] }));
  });
  it('signed-out users cannot write anything', () => assertFails(setDoc(doc(anon(), 'requests/r3'), { items: [] })));
});
