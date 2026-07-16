package com.paypal.android.venmo.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

internal class VenmoAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: VenmoCheckoutEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            orderId = orderId,
            appSwitchEnabled = true
        )
    }
}
