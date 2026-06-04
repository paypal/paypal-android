package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsParams
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.AnalyticsServiceWrapper

internal class PayPalWebAnalytics(
    override val analyticsService: AnalyticsService
) : AnalyticsServiceWrapper {

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
