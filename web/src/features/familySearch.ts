// F3: public family search (D6). Offline = this shelter only (labelled); online = all shelters in the region.
import { familySearch, groupDuplicates, type PublicRec } from '../domain/dedupe';
import { offlineVariants, parseLookFor, searchVariants } from '../domain/family';
import { type Deps, online } from './deps';

export function createFamilySearch(d: Deps) {
  const local = (): PublicRec[] => {
    const s = d.store.get();
    return s.people.filter(p => p.confirmedBy).map(p => ({ id: p.id, name: p.nameAsWritten, age: p.age, shelterId: s.shelter.id, status: p.status, hidden: p.hidden }));
  };
  async function cloud(): Promise<PublicRec[]> {
    if (!online(d)) return [];
    try {
      return (await d.sync.list('public')).map((r, i) => ({ id: String(r.id ?? i), name: String(r.name), age: (r.age as number) ?? null, shelterId: String(r.shelterId), status: String(r.status), hidden: !!r.hidden }));
    } catch { return []; }
  }
  return {
    async search(q: string) {
      const remote = await cloud();
      const seen = new Set<string>();
      const hits = familySearch(q, [...local(), ...remote]).filter(h => { const k = `${h.name}|${h.shelterId}`; return !seen.has(k) && !!seen.add(k); });
      return { hits, cloud: remote.length > 0 };
    },
    /** Hybrid family search. Device: understand "my grandmother Nurhayati, about 80" privately (name, age, place).
     *  Cloud (online, only the name): Indonesian/Acehnese spellings. Matching is deterministic; a person confirms. */
    async smart(text: string) {
      const q = await d.router.run<{ name: string; age: number | null; place: string | null }>('family_query', { text: text.trim() });
      const ai = (q.status === 'ok' || q.status === 'draft') && q.source === 'device' && q.data?.name ? q.data : null;
      const parsed = ai ?? parseLookFor(text);
      const name = parsed.name;
      let variants = offlineVariants(name, d.rules.nameRules), variantsBy: 'cloud' | 'offline' = 'offline';
      if (online(d)) {
        const v = await d.router.run<{ variants: string[] }>('name_variants', { text: name });
        if (v.status === 'ok' && v.data) { variants = [...new Set([...v.data.variants.filter(x => x.trim() && x.toLowerCase() !== name.toLowerCase()), ...variants])]; variantsBy = 'cloud'; }
      }
      const remote = await cloud();
      return { name, age: parsed.age ?? null, place: parsed.place ?? null, parsedBy: ai ? 'device' as const : 'rules' as const, variants, variantsBy,
        hits: searchVariants(name, variants, parsed.age ?? null, [...local(), ...remote]), cloud: remote.length > 0 };
    },
    /** Organizer view of the cross-shelter merge: groups that look like the same person. */
    async duplicates() { const all = [...local(), ...(await cloud())]; return groupDuplicates(all).filter(g => g.length > 1); },
  };
}
