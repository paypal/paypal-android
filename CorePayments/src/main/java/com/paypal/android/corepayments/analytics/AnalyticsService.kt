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

    @Suppress("LongParameterList")
    fun sendAnalyticsEvent(
        name: String,
        orderId: String? = null,
        buttonType: String? = null,
        appSwitchEnabled: Boolean? = null,
        shopperSessionId: String? = null,
        appSwitchUrl: String? = null,
        errorDescription: String? = null,
        startTime: Long? = null,
        isCachedSession: Boolean? = null,
        isVaultRequest: Boolean? = null,
        endTime: Long? = null,
        endpoint: String? = null,
        presentationType: String? = null,
        flow: String? = null
    ) {
        // TODO: send analytics event using WorkManager (supports coroutines) to avoid lint error
        // thrown because we don't use the Deferred result
        scope.launch {
            val timestamp = System.currentTimeMillis()
            try {
                val deviceData = deviceInspector.inspect()
                val analyticsEventData = AnalyticsEventData(
                    environment.name.lowercase(),
                    name,
                    timestamp,
                    orderId = orderId,
                    buttonType = buttonType,
                    appSwitchEnabled = appSwitchEnabled,
                    shopperSessionId = shopperSessionId,
                    appSwitchUrl = appSwitchUrl,
                    errorDescription = errorDescription,
                    startTime = startTime,
                    isCachedSession = isCachedSession,
                    isVaultRequest = isVaultRequest,
                    endTime = endTime,
                    endpoint = endpoint,
                    presentationType = presentationType,
                    flow = flow
                )
                val response = trackingEventsAPI.sendEvent(analyticsEventData, deviceData)
                response.error?.message?.let { errorMessage ->
                    Log.d("[PayPal SDK]", "Failed to send analytics: $errorMessage")
                }
            } catch (e: PayPalSDKError) {
                Log.d(
                    "[PayPal SDK]",
                    "Failed to send analytics due to missing clientId: ${e.message}"
                )
            }
        }
    }
}
