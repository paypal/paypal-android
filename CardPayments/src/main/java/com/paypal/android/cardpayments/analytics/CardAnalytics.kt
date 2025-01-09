package com.paypal.android.cardpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

internal class CardAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: ApproveOrderEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(event.value, orderId)
    }

    fun notify(event: VaultEvent, setupTokenId: String?) {
        analyticsService.sendAnalyticsEvent(event.value, setupTokenId)
    }
}
