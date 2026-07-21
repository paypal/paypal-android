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
        shopperSessionExpiration: String? = null,
        matchedAuthenticationMethods: List<String>? = null,
        appSwitchUrl: String? = null,
        fallbackUrl: String? = null,
        errorDescription: String? = null,
        isCachedSession: Boolean? = null,
        isVault: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        endpoint: String? = null,
        presentationType: String? = null,
        flow: String? = null,
        appSwitchEligible: Boolean? = null,
        ineligibleReason: String? = null,
        merchantId: String? = null,
        bnCode: String? = null,
        clientId: String? = null,
        userAction: String? = null,
        paypalInstalled: String? = null,
        returnAppUrl: String? = null,
        cancelAppUrl: String? = null,
        fallbackSchemeUrl: String? = null,
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
                    shopperSessionExpirationAt = shopperSessionExpiration,
                    matchedAuthenticationMethods = matchedAuthenticationMethods,
                    appSwitchUrl = appSwitchUrl,
                    checkoutFallbackUrl = fallbackUrl,
                    errorDescription = errorDescription,
                    isCachedSession = isCachedSession,
                    isVault = isVault,
                    startTime = startTime,
                    appSwitchEligible = appSwitchEligible,
                    ineligibleReason = ineligibleReason,
                    merchantId = merchantId,
                    bnCode = bnCode,
                    clientId = clientId,
                    userAction = userAction,
                    paypalInstalled = paypalInstalled,
                    returnAppUrl = returnAppUrl,
                    cancelAppUrl = cancelAppUrl,
                    fallbackSchemeUrl = fallbackSchemeUrl,
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
