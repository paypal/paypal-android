package com.paypal.android.cardpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventParams
import com.paypal.android.corepayments.analytics.AnalyticsService

internal class CardAnalytics(private val analyticsService: AnalyticsService) {
    fun notify(event: ApproveOrderEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(orderId = orderId)
        )
    }

    fun notify(event: VaultEvent, vaultSetupToken: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(vaultSetupToken = vaultSetupToken)
        )
    }
}
