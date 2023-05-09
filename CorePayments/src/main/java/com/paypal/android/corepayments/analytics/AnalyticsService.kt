package com.paypal.android.corepayments.analytics

import android.content.Context
import android.util.Log
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.SecureTokenServiceAPI
import com.paypal.android.corepayments.TrackingEventsAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.UUID

class AnalyticsService internal constructor(
    private val deviceInspector: DeviceInspector,
    private val environment: Environment,
    private val trackingEventsAPI: TrackingEventsAPI,
    private val secureTokenServiceAPI: SecureTokenServiceAPI,
    private val scope: CoroutineScope
) {

    constructor(
        context: Context,
        coreConfig: CoreConfig,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) :
            this(
                DeviceInspector(context),
                coreConfig.environment,
                TrackingEventsAPI(coreConfig),
                SecureTokenServiceAPI(coreConfig),
                CoroutineScope(dispatcher)
            )

fun sendAnalyticsEvent(name: String, orderId: String?) {
        // TODO: send analytics event using WorkManager (supports coroutines) to avoid lint error
        // thrown because we don't use the Deferred result
        scope.async {
            val timestamp = System.currentTimeMillis()
            try {
                val clientID = secureTokenServiceAPI.fetchCachedOrRemoteClientID()
                val deviceData = deviceInspector.inspect()
                val analyticsEventData = AnalyticsEventData(
                    clientID,
                    environment.name.lowercase(),
                    name,
                    timestamp,
                    orderId
                )
                val response = trackingEventsAPI.sendEvent(analyticsEventData, deviceData)
                response.error?.message?.let { errorMessage ->
                    Log.d("[PayPal SDK]", "Failed to send analytics: $errorMessage")
                }
            } catch (e: PayPalSDKError) {
                Log.d(
                    "[PayPal SDK]",
                    "Failed to send analytics due to missing clientID: ${e.message}"
                )
            }
        }
    }
}
