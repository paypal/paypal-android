package com.paypal.android.corepayments.analytics

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.TrackingEventsAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AnalyticsService internal constructor(
    private val deviceInspector: DeviceInspector,
    private val environment: Environment,
    private val trackingEventsAPI: TrackingEventsAPI,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "[PayPal SDK]"
    }

    constructor(context: Context, coreConfig: CoreConfig) :
            this(context, coreConfig, Dispatchers.IO)

    @VisibleForTesting
    internal constructor(
        context: Context,
        coreConfig: CoreConfig,
        dispatcher: CoroutineDispatcher
    ) :
            this(
                DeviceInspector(context),
                coreConfig.environment,
                TrackingEventsAPI(coreConfig),
                CoroutineScope(dispatcher)
            )

    fun sendAnalyticsEvent(name: String, params: AnalyticsEventParams = AnalyticsEventParams()) {
        // TODO: send analytics event using WorkManager (supports coroutines) to avoid lint error
        // thrown because we don't use the Deferred result
        scope.launch {
            val timestamp = System.currentTimeMillis()
            try {
                val deviceData = deviceInspector.inspect()
                val analyticsEventData = AnalyticsEventData(
                    environment = environment.name.lowercase(),
                    eventName = name,
                    timestamp = timestamp,
                    params = params
                )
                val response = trackingEventsAPI.sendEvent(analyticsEventData, deviceData)
                response.error?.message?.let { errorMessage ->
                    Log.w(TAG, "Failed to send analytics: $errorMessage")
                }
            } catch (e: PayPalSDKError) {
                Log.e(TAG, "Failed to send analytics due to missing clientId: ${e.message}")
            }
        }
    }
}
