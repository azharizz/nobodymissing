import { collection, doc, getDocs, setDoc, type Firestore } from 'firebase/firestore';
import type { SyncPort } from '../ports';

/** Firestore adapter. Writes are idempotent (doc id = job id), so retries are safe. */
export class FirestoreSync implements SyncPort {
  constructor(private db: Firestore) {}
  async put(path: string, data: object) { await setDoc(doc(this.db, path), { ...data, updatedAt: Date.now() }, { merge: true }); }
  async list(col: string) { return (await getDocs(collection(this.db, col))).docs.map(d => ({ id: d.id, ...d.data() })); }
}
