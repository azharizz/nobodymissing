package org.nobodymissing.adapters

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import org.nobodymissing.ports.SyncPort

class FirestoreSync : SyncPort {
    override suspend fun put(path: String, data: Map<String, Any?>) {
        Firebase.firestore.document(path).set(data + ("updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
    }
}
