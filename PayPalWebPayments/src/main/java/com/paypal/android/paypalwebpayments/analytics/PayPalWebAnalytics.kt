package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsParams
import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalWebAnalytics(internal val analyticsService: AnalyticsService) {

    fun notify(event: CheckoutEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = buildParams(orderId)
        )
    }

    fun notify(event: VaultEvent, setupTokenId: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = buildParams(setupTokenId)
        )
    }

    private fun buildParams(orderId: String?): Map<String, String> =
        orderId?.let { mapOf(AnalyticsParams.ORDER_ID to it) } ?: emptyMap()
}
