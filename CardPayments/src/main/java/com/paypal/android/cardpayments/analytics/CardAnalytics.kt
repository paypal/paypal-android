package com.paypal.android.cardpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventParams
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.analytics.AnalyticsServiceWrapper

internal class CardAnalytics(
    override val analyticsService: AnalyticsService
) : AnalyticsServiceWrapper {

    fun notify(event: ApproveOrderEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(orderId = orderId)
        )
    }

    fun notify(event: VaultEvent, setupTokenId: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            params = AnalyticsEventParams(setupTokenId = setupTokenId)
        )
    }
}
