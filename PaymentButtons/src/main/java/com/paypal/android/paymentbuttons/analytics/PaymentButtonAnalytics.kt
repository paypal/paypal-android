package com.paypal.android.paymentbuttons.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventParams
import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PaymentButtonAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: PaymentButtonEvent, buttonType: String) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(buttonType = buttonType)
        )
    }
}
