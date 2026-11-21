import { expect, it } from 'vitest';
import { scoreCards } from './score';

const t = { id: 'B03', lang: 'en', text: '', icons: ['medicine'], time: '08:00', place: 'health post' };
it('accepts a correct card with "8 am" normalised', () => expect(scoreCards(t, [{ icon: 'medicine', title: 'Meds', time: '8 am', place: 'Health Post' }]).all).toBe(true));
it('rejects a wrong icon', () => expect(scoreCards(t, [{ icon: 'food', title: 'x', time: '08:00', place: 'health post' }]).icon).toBe(false));
it('rejects a wrong time', () => expect(scoreCards(t, [{ icon: 'medicine', title: 'x', time: '09:00', place: 'health post' }]).time).toBe(false));
it('no cards scores zero', () => expect(scoreCards(t, null).all).toBe(false));
