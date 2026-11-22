// Anti-corruption layer: every AI output is parsed + schema-validated before it enters the domain.
// A small JSON Schema subset (type, properties, required, items, enum, minimum, maximum, maxLength, maxItems),
// implemented identically on Android so both platforms accept and reject the same outputs.
import roster from '../../../shared/schemas/roster.json';
import careCard from '../../../shared/schemas/careCard.json';
import announcementCard from '../../../shared/schemas/announcementCard.json';
import fieldReport from '../../../shared/schemas/fieldReport.json';
import request from '../../../shared/schemas/request.json';
import translation from '../../../shared/schemas/translation.json';
import medCheck from '../../../shared/schemas/medCheck.json';
import findOpen from '../../../shared/schemas/findOpen.json';
import askKit from '../../../shared/schemas/askKit.json';
import helpTriage from '../../../shared/schemas/helpTriage.json';

export const SCHEMAS = { roster, careCard, announcementCard, fieldReport, request, translation, medCheck, findOpen, askKit, helpTriage } as Record<string, Schema>;
export type Schema = {
  type?: string | string[]; properties?: Record<string, Schema>; required?: string[]; items?: Schema;
  enum?: unknown[]; minimum?: number; maximum?: number; maxLength?: number; maxItems?: number;
};

const typeOf = (v: unknown) =>
  v === null ? 'null' : Array.isArray(v) ? 'array' : typeof v === 'number' ? (Number.isInteger(v) ? 'integer' : 'number') : typeof v;

export function validate(v: unknown, s: Schema, path = '$', errors: string[] = []): string[] {
  if (s.type) {
    const types = Array.isArray(s.type) ? s.type : [s.type];
    const t = typeOf(v);
    if (!types.includes(t) && !(t === 'integer' && types.includes('number'))) { errors.push(`${path}: expected ${types.join('|')}`); return errors; }
  }
  if (s.enum && !s.enum.includes(v)) errors.push(`${path}: not allowed`);
  if (typeof v === 'number') {
    if (s.minimum != null && v < s.minimum) errors.push(`${path}: < ${s.minimum}`);
    if (s.maximum != null && v > s.maximum) errors.push(`${path}: > ${s.maximum}`);
  }
  if (typeof v === 'string' && s.maxLength != null && v.length > s.maxLength) errors.push(`${path}: too long`);
  if (Array.isArray(v)) {
    if (s.maxItems != null && v.length > s.maxItems) errors.push(`${path}: too many items`);
    if (s.items) v.forEach((x, i) => validate(x, s.items!, `${path}[${i}]`, errors));
  }
  if (v && typeof v === 'object' && !Array.isArray(v)) {
    const o = v as Record<string, unknown>;
    for (const k of s.required ?? []) if (!(k in o)) errors.push(`${path}.${k}: required`);
    for (const [k, sub] of Object.entries(s.properties ?? {})) if (k in o) validate(o[k], sub, `${path}.${k}`, errors);
  }
  return errors;
}

/** Small models wrap JSON in prose or code fences: take the outermost {...}. */
export function parseJson(raw: string): unknown {
  try { return JSON.parse(raw); } catch { /* fall through */ }
  const a = raw.indexOf('{'), b = raw.lastIndexOf('}');
  if (a < 0 || b <= a) return undefined;
  try { return JSON.parse(raw.slice(a, b + 1)); } catch { return undefined; }
}

export function parseAndValidate<T = unknown>(raw: string, schema: Schema): { valid: boolean; value?: T; errors: string[] } {
  const v = parseJson(raw);
  if (v === undefined) return { valid: false, errors: ['not JSON'] };
  const errors = validate(v, schema);
  return errors.length ? { valid: false, errors } : { valid: true, value: v as T, errors };
}
