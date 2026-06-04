package com.paypal.android.cardpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsParams
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.AnalyticsServiceWrapper

internal class CardAnalytics(
    override val analyticsService: AnalyticsService
) : AnalyticsServiceWrapper {

    fun notify(event: ApproveOrderEvent, orderId: String?) {
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
