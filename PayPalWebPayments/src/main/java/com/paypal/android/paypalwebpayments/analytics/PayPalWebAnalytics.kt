package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalWebAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: CheckoutEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(event.value, orderId)
    }

    fun notify(event: VaultEvent, setupTokenId: String?) {
        analyticsService.sendAnalyticsEvent(event.value, setupTokenId)
    }
}
