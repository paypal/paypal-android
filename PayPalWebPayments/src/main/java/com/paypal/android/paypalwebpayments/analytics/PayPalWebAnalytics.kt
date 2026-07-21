package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalWebAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: CheckoutEvent, params: AppSwitchAnalyticsEventParams, errorDescription: String? = null) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            eventData = params.toAnalyticsEventData(
                orderId = params.checkoutOrderId,
                errorDescription = errorDescription,
            ),
        )
    }

    fun notify(event: VaultEvent, params: AppSwitchAnalyticsEventParams, errorDescription: String? = null) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            eventData = params.toAnalyticsEventData(
                orderId = params.vaultSetupTokenId,
                errorDescription = errorDescription,
            ),
        )
    }

    fun notify(
        event: CreatePayPalSessionEvent,
        params: AppSwitchAnalyticsEventParams,
        errorDescription: String? = null,
    ) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            eventData = params.toAnalyticsEventData(errorDescription = errorDescription),
        )
    }

    fun notifyApiRequestLatency(endpoint: String, startTime: Long, endTime: Long) {
        analyticsService.sendAnalyticsEvent(
            name = LatencyEvent.API_REQUEST_LATENCY.value,
            eventData = AnalyticsEventData(
                endpoint = endpoint,
                startTime = startTime,
                endTime = endTime,
            ),
        )
    }

    fun notifyUserPerceivedLatency(
        flow: String,
        presentationType: String,
        startTime: Long,
        endTime: Long
    ) {
        analyticsService.sendAnalyticsEvent(
            name = LatencyEvent.USER_PERCEIVED_LATENCY.value,
            eventData = AnalyticsEventData(
                flow = flow,
                presentationType = presentationType,
                startTime = startTime,
                endTime = endTime,
            ),
        )
    }
}

private fun AppSwitchAnalyticsEventParams.toAnalyticsEventData(
    orderId: String? = null,
    errorDescription: String? = null,
) = AnalyticsEventData(
    orderId = orderId,
    appSwitchEnabled = appSwitchEnabled,
    shopperSessionId = shopperSession?.shopperSessionConfig?.id,
    shopperSessionExpiration = shopperSession?.shopperSessionConfig?.expiresAt,
    matchedAuthenticationMethods = shopperSession?.matchedAuthenticationMethods,
    appSwitchUrl = appSwitchUrl,
    checkoutFallbackUrl = shopperSession?.checkoutFallbackUrl,
    errorDescription = errorDescription,
    isCachedSession = isCachedSession,
    isVault = isVault,
    appSwitchEligible = shopperSession?.appSwitchEligible,
    ineligibleReason = shopperSession?.ineligibleReason,
    merchantId = merchantId,
    bnCode = bnCode,
    clientId = clientId,
    userAction = userActionValue,
    paypalInstalled = paypalInstalled,
    returnAppUrl = returnAppUrl,
    cancelAppUrl = cancelAppUrl,
    fallbackSchemeUrl = fallbackSchemeUrl,
)
