package com.paypal.android

import android.app.Application
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsServiceRegistry
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AnalyticsServiceRegistry.initialize(this, CoreConfig(SDKSampleServerAPI.clientId))
    }
}
