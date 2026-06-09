package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventParams
import com.paypal.android.corepayments.analytics.AnalyticsService

internal class PayPalWebAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: CheckoutEvent, orderId: String?, appSwitchEnabled: Boolean) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(orderId = orderId, appSwitchEnabled = appSwitchEnabled)
        )
    }

    fun notify(event: VaultEvent, vaultSetupToken: String?, appSwitchEnabled: Boolean) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(vaultSetupToken = vaultSetupToken, appSwitchEnabled = appSwitchEnabled)
        )
    }
}
