package org.nobodymissing

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.remoteConfig
import org.nobodymissing.adapters.AdapterFactory
import org.nobodymissing.features.FeatureFactory
import org.nobodymissing.features.Features
import org.nobodymissing.features.Shared
import org.nobodymissing.features.autoSave
import org.nobodymissing.features.loadFrom

import kotlinx.coroutines.launch

class NobodyMissingApp : Application() {
    lateinit var features: Features
    override fun onCreate() {
        super.onCreate()
        val adapters = AdapterFactory.create(this)
        // Remote Config: use the last activated value now (works offline), fetch fresh values for the next start.
        val remoteRules = if (FirebaseApp.getApps(this).isEmpty()) null else Firebase.remoteConfig.run {
            fetchAndActivate()
            getString("rules").takeIf { it.isNotBlank() }
        }
        features = FeatureFactory.create(adapters, Shared.from(this, remoteRules))
        // Saved state first (the phone may have killed the app); else demo mode shows the synthetic Aceh shelter (a real shelter starts empty).
        val saved = java.io.File(filesDir, "state.bin")
        if (!features.loadFrom(saved) && adapters.env == "test") features.loadDemo()
        features.autoSave(saved)
        // First question fast: load the on-device model before anyone types.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch { features.router.warm() }
    }
}
