package org.nobodymissing

import android.app.Application
import org.nobodymissing.adapters.AdapterFactory
import org.nobodymissing.features.FeatureFactory
import org.nobodymissing.features.Features
import org.nobodymissing.features.Shared

class NobodyMissingApp : Application() {
    lateinit var features: Features
    override fun onCreate() {
        super.onCreate()
        features = FeatureFactory.create(AdapterFactory.create(this), Shared.from(this))
    }
}
