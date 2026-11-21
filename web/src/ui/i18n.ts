// UI strings live in shared/i18n (one file per language), read by web and Android so they can't drift apart.
import en from '../../../shared/i18n/en.json';
import id from '../../../shared/i18n/id.json';

export const dict = { en, id };
export type T = typeof en;
export const fmt = (s: string, vars: Record<string, string | number>) => s.replace(/\{(\w+)\}/g, (m, k) => (k in vars ? String(vars[k]) : m));
