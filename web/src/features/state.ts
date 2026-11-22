// App state + a tiny external store (read with useSyncExternalStore; no useEffect anywhere).
import type { DrugMatch } from '../domain/drugMatcher';
import type { PriorityInput, Tier } from '../domain/priority';
import type { JobState } from '../domain/queue';
import type { SignalState } from '../domain/scout';
import type { ImageInput, StoragePort } from '../ports';

export type Person = {
  id: string; nameAsWritten: string; age: number | null; sex: 'M' | 'F' | null; flags: string[]; suggestedFlags: string[];
  notes: string | null; status: 'safe' | 'injured' | 'moved'; needsReview: boolean; inferredBy: 'device' | 'cloud' | 'staff';
  confirmedBy: 'staff' | null; hidden?: boolean;
};
export type MedEntry = {
  nameAsWritten: string; strength: string | null; doseAsWritten: string | null; timesPerDay: number | null; qtyLeft: number | null;
  match: DrugMatch; generic: string | null; confirmedBy: 'self' | 'family' | 'staff' | 'medic' | null; inferredBy?: 'device' | 'cloud';
};
export type MedCheck = {
  items: { name: string; generic: string | null; alsoKnownAs?: string[]; swaps?: string[]; cantMiss?: boolean; note?: string | null }[];
  interactions: { a: string; b: string; severity: 'low' | 'moderate' | 'high'; note?: string | null }[];
  cross: { name: string; offline: string | null; status: 'agree' | 'mismatch' | 'cloud_only' | 'offline_only' | 'unknown' }[];
  source: 'cloud'; at: number; pending: boolean;
};
export type OpenPlace = { name: string; kind?: string | null; distanceKm?: number | null; openNow?: boolean | null; hours?: string | null; note?: string | null; uri: string | null; placeId: string | null };
export type CareCard = { personId: string; meds: MedEntry[]; conditions?: string[]; allergies?: string[]; check?: MedCheck };
export type Announcement = {
  id: string; text: string; lang: 'en' | 'id'; icons: string[]; times: string[]; place: string | null;
  cards: { icon: string; title: string; time?: string | null; place?: string | null; detail?: string | null }[] | null;
  source: 'device' | 'cloud' | 'keywords'; draft: boolean; shown: boolean; told: string[]; createdAt: number;
};
export type FieldEntry = {
  id: string; name: string; location: { lat: number; lng: number } | null; lastContactAt: number | null;
  person: Omit<PriorityInput, 'hazard' | 'hoursSinceContact' | 'override'> & { hazard?: PriorityInput['hazard'] };
  override: { tier: Tier; reason: string; at: number } | null; status: 'unchecked' | 'safe_home' | 'needs_evacuation' | 'moved_to_shelter';
  needsReview?: boolean; pickup?: { needs: string[]; people: number; at: number };
};
export type Job = { id: string; type: string; payload: unknown; path: string | null; createdAt: number; state: JobState; retries: number };
export type Help = { id: string; kind: string; spot: string; at: number; done: boolean; text?: string; summary?: string; urgent?: boolean; by?: 'device' | 'keywords' };
export type BatchItem = { id: string; kind: 'roster' | 'med'; personId?: string; image: ImageInput; approved: boolean };

export type State = {
  shelter: { id: string; name: string }; people: Person[]; cards: CareCard[]; round: Record<string, 'taken' | 'refused' | 'missing'>;
  announcements: Announcement[]; field: FieldEntry[]; jobs: Job[]; help: Help[]; batch: BatchItem[];
  privateMode: boolean; signal: SignalState; scoutRun: number; scoutAlert: boolean; lastSignalAt: number | null;
  lang: 'en' | 'id'; volunteerPos: { lat: number; lng: number } | null;
  open: { need: string; places: OpenPlace[]; at: number; pending: boolean } | null;
  warning: (import('../domain/firstHours').Warning & { text: string; at: number; sources: { title?: string; uri?: string }[] }) | null;
  myPos: { lat: number; lng: number } | null;
  myPickup: (import('../domain/firstHours').PickupRecord & { jobId: string }) | null;
  /** Live hybrid counter, saved; it says "Today", so a new day starts at 0. */
  counts: { day: string; device: number; cloud: number };
  /** Where Kit ran: one line per AI call (task, device/cloud, why, ms). Last 60, today only shown. */
  kitLog: (import('./inferenceRouter').RunLog & { at: number })[];
  familyGroup: { fam: string; fams: string[]; me: import('../domain/family').Member; members: import('../domain/family').Member[]; via: Record<string, 'qr' | 'online'> } | null;
};

/** A real shelter starts empty: people come from scanned lists, never from bundled data. */
export function initialState(): State {
  return {
    shelter: { id: 'shelter-1', name: 'Shelter' }, people: [], cards: [], round: {}, announcements: [], field: [], jobs: [], help: [], batch: [],
    privateMode: false, signal: 'no_signal', scoutRun: 0, scoutAlert: false, lastSignalAt: null, lang: 'en', volunteerPos: null, open: null, familyGroup: null, warning: null, myPos: null, myPickup: null, counts: { day: '', device: 0, cloud: 0 }, kitLog: [],
  };
}

export type Seed = (now: number) => Partial<State>;

export type Store = ReturnType<typeof createStore>;

/** Photos (batch) stay in memory only: never persisted, never synced. */
export function createStore(storage: StoragePort, now: number, seed?: Seed) {
  const saved = storage.load<Partial<State>>('state');
  let state: State = { ...initialState(), ...(saved ?? seed?.(now) ?? {}), batch: [], scoutAlert: false };
  const listeners = new Set<() => void>();
  return {
    get: () => state,
    set(fn: (s: State) => State) {
      state = fn(state);
      const { batch: _b, ...persist } = state;
      storage.save('state', persist);
      listeners.forEach(l => l());
    },
    subscribe(l: () => void) { listeners.add(l); return () => { listeners.delete(l); }; },
  };
}
