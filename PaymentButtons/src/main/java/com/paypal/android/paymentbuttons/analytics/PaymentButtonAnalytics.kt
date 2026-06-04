package com.paypal.android.paymentbuttons.analytics

import com.paypal.android.corepayments.analytics.AnalyticsParams
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.AnalyticsServiceWrapper

internal class PaymentButtonAnalytics(
    override val analyticsService: AnalyticsService
) : AnalyticsServiceWrapper {

    fun notify(event: PaymentButtonEvent, buttonType: String) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = mapOf(AnalyticsParams.BUTTON_TYPE to buttonType)
        )
    }
}
