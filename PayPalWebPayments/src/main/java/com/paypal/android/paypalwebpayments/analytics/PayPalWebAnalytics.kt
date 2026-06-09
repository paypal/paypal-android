package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventParams
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.AnalyticsServiceWrapper

internal class PayPalWebAnalytics(
    override val analyticsService: AnalyticsService
) : AnalyticsServiceWrapper {

    fun notify(event: CheckoutEvent, orderId: String?, appSwitchEnabled: Boolean) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(orderId = orderId, appSwitchEnabled = appSwitchEnabled)
        )
    }

    fun notify(event: VaultEvent, setupTokenId: String?, appSwitchEnabled: Boolean) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(setupTokenId = setupTokenId, appSwitchEnabled = appSwitchEnabled)
        )
    }
}
