package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalWebAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(
        event: CheckoutEvent,
        orderId: String?,
        appSwitchEnabled: Boolean,
        shopperSessionId: String? = null,
        appSwitchUrl: String? = null,
        errorDescription: String? = null,
    ) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            orderId = orderId,
            appSwitchEnabled = appSwitchEnabled,
            shopperSessionId = shopperSessionId,
            appSwitchUrl = appSwitchUrl,
            errorDescription = errorDescription,
        )
    }

    fun notify(
        event: VaultEvent,
        setupTokenId: String?,
        appSwitchEnabled: Boolean,
        shopperSessionId: String? = null,
        appSwitchUrl: String? = null,
        errorDescription: String? = null,
    ) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            orderId = setupTokenId,
            appSwitchEnabled = appSwitchEnabled,
            shopperSessionId = shopperSessionId,
            appSwitchUrl = appSwitchUrl,
            errorDescription = errorDescription,
        )
    }

    fun notify(
        event: CreatePayPalSessionEvent,
        shopperSessionId: String? = null,
        isCachedSession: Boolean? = null,
        isVaultRequest: Boolean? = null,
        errorDescription: String? = null,
        startTime: Long? = null,
    ) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            shopperSessionId = shopperSessionId,
            isCachedSession = isCachedSession,
            isVaultRequest = isVaultRequest,
            errorDescription = errorDescription,
            startTime = startTime,
        )
    }

    fun notifyApiRequestLatency(endpoint: String, startTime: Long, endTime: Long) {
        analyticsService.sendAnalyticsEvent(
            name = LatencyEvent.API_REQUEST_LATENCY.value,
            endpoint = endpoint,
            startTime = startTime,
            endTime = endTime
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
            flow = flow,
            presentationType = presentationType,
            startTime = startTime,
            endTime = endTime
        )
    }
}
