package org.nobodymissing.adapters

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.nobodymissing.BuildConfig
import org.nobodymissing.ports.*

data class Adapters(val env: String, val inference: InferencePort, val sync: SyncPort, val connectivity: ConnectivityPort)

/** AdapterFactory: prod (Firebase) / emulator (Emulator Suite) / test (fakes). The only place adapters are built. */
object AdapterFactory {
    fun create(context: Context, env: String = defaultEnv(context), shelterId: String = "SD3", role: String = "volunteer"): Adapters = when (env) {
        "test" -> Adapters(env, FakeInference("unavailable"), MemorySync(), FakeConnectivity(false))
        else -> {
            // App Check is required for AI Logic: debug provider in debug builds (register the token), Play Integrity in release.
            Firebase.appCheck.installAppCheckProviderFactory(
                if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance() else PlayIntegrityAppCheckProviderFactory.getInstance())
            if (env == "emulator") { Firebase.firestore.useEmulator("10.0.2.2", 8080); Firebase.auth.useEmulator("10.0.2.2", 9099) }
            // Silent sign-in (D14), then join the shelter as a member so Firestore rules accept staff writes.
            Firebase.auth.signInAnonymously().addOnSuccessListener { r ->
                Firebase.firestore.document("shelters/$shelterId/members/${r.user!!.uid}").set(mapOf("role" to role, "joinedAt" to System.currentTimeMillis()))
            }
            Adapters(env, FirebaseAiLogicInference(), FirestoreSync(), HttpConnectivity())
        }
    }
    /** No google-services.json -> no FirebaseApp -> fakes, so the app still opens for testing. */
    private fun defaultEnv(context: Context) = if (FirebaseApp.getApps(context).isEmpty()) "test" else "prod"
}
