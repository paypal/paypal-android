package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalWebAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: CheckoutEvent, orderId: String?, appSwitchEnabled: Boolean) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            orderId = orderId,
            appSwitchEnabled = appSwitchEnabled
        )
    }

    fun notify(event: VaultEvent, setupTokenId: String?, appSwitchEnabled: Boolean) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            orderId = setupTokenId,
            appSwitchEnabled = appSwitchEnabled
        )
    }
}
