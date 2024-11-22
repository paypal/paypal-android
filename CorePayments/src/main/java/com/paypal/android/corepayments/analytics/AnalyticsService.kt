package com.paypal.android.corepayments.analytics

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreSDKResult
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

    fun sendAnalyticsEvent(name: String, orderId: String? = null, buttonType: String? = null) {
        // TODO: send analytics event using WorkManager (supports coroutines) to avoid lint error
        // thrown because we don't use the Deferred result
        scope.launch {
            val timestamp = System.currentTimeMillis()
            val deviceData = deviceInspector.inspect()
            val analyticsEventData = AnalyticsEventData(
                environment.name.lowercase(),
                name,
                timestamp,
                orderId = orderId,
                buttonType = buttonType
            )
            when (val result = trackingEventsAPI.sendEvent(analyticsEventData, deviceData)) {
                is CoreSDKResult.Failure -> {
                    val message = result.value.message
                    Log.d("[PayPal SDK]", "Failed to send analytics: $message")
                }

                is CoreSDKResult.Success -> {
                    // success! do nothing
                }
            }
        }
    }
}
