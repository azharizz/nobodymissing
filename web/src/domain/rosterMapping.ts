// Map validated AI roster rows into person drafts. AI never confirms anything; staff do (6.8b).
export type RosterRow = { name: string; age?: number | null; sex?: 'M' | 'F' | null; flags?: string[]; notes?: string | null; confidence: number };
export type PersonDraft = {
  nameAsWritten: string; age: number | null; sex: 'M' | 'F' | null; suggestedFlags: string[]; notes: string | null;
  needsReview: boolean; inferredBy: 'device' | 'cloud'; confirmedBy: null;
};

export function mapRosterRows(rows: RosterRow[], source: 'device' | 'cloud', r: { elderlyAge: number; reviewConfidence: number }): PersonDraft[] {
  return rows.map(row => {
    const flags = [...(row.flags ?? [])];
    if (row.age != null && row.age >= r.elderlyAge && !flags.includes('elderly')) flags.push('elderly');
    return {
      nameAsWritten: row.name, age: row.age ?? null, sex: row.sex ?? null, suggestedFlags: flags, notes: row.notes ?? null,
      needsReview: row.confidence < r.reviewConfidence, inferredBy: source, confirmedBy: null,
    };
  });
}
