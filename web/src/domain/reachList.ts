// Reach list per announcement (F8): people who must be told in person, from roster flags + age.
type P = { id: string; age: number | null; flags: string[] };

export function reachList(people: P[], told: string[], r: { reachFlags: string[]; reachAge: number }) {
  const list = people
    .map(p => ({
      id: p.id,
      reasons: [...r.reachFlags.filter(f => p.flags.includes(f)), ...((p.age ?? 0) >= r.reachAge ? [`age ${r.reachAge}+`] : [])],
      told: told.includes(p.id),
    }))
    .filter(x => x.reasons.length);
  return { list, remaining: list.filter(x => !x.told).length };
}
