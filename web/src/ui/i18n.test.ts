import { expect, it } from 'vitest';
import en from '../../../shared/i18n/en.json';
import id from '../../../shared/i18n/id.json';
import { fmt } from './i18n';

const vars = (s: string) => (s.match(/\{\w+\}/g) ?? []).sort();
it('EN and ID have the same keys', () => expect(Object.keys(id).sort()).toEqual(Object.keys(en).sort()));
it('every translation keeps the same {placeholders}', () => {
  for (const k of Object.keys(en) as (keyof typeof en)[]) expect(vars(id[k]), k).toEqual(vars(en[k]));
});
it('fmt fills placeholders', () => expect(fmt(en.counter, { device: 2, cloud: 1, waiting: 0 })).toBe('Today: 2 on device · 1 cloud · 0 waiting'));
