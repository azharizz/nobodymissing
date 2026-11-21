package org.nobodymissing

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.remoteConfig
import org.nobodymissing.adapters.AdapterFactory
import org.nobodymissing.features.FeatureFactory
import org.nobodymissing.features.Features
import org.nobodymissing.features.Shared

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
    }
}
