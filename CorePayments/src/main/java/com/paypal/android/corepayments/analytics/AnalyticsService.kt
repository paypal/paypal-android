package com.paypal.android.corepayments.analytics

import android.content.Context
import android.util.Log
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.SecureTokenServiceAPI
import com.paypal.android.corepayments.TrackingEventsAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

class AnalyticsService internal constructor(
    private val deviceInspector: DeviceInspector,
    private val environment: Environment,
    private val trackingEventsAPI: TrackingEventsAPI,
    private val secureTokenServiceAPI: SecureTokenServiceAPI
) {

    constructor(context: Context, coreConfig: CoreConfig) :
            this(
                DeviceInspector(context),
                coreConfig.environment,
                TrackingEventsAPI(coreConfig),
                SecureTokenServiceAPI(coreConfig)
            )

    fun sendAnalyticsEvent(name: String) = GlobalScope.launch {
        val timestamp = System.currentTimeMillis()

        try {
            val clientID = secureTokenServiceAPI.fetchCachedOrRemoteClientID()
            val analyticsEventData = AnalyticsEventData(
                clientID,
                environment.name.lowercase(),
                name,
                timestamp,
                sessionId,
                deviceInspector.inspect()
            )
            val response = trackingEventsAPI.sendEvent(analyticsEventData)
            if (!response.isSuccessful) {
                Log.d("[PayPal SDK]", "Failed to send analytics: ${response.error?.message}")
            }
        } catch (e: PayPalSDKError) {
            Log.d(
                "[PayPal SDK]",
                "Failed to send analytics due to missing clientID: ${e.message}"
            )
        }
    }

    companion object {
        private val sessionId = UUID.randomUUID().toString()
    }
}
