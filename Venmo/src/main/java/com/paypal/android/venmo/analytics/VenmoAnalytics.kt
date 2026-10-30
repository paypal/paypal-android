package com.paypal.android.venmo.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.common.DeviceInspector

internal class VenmoAnalytics(
    private val analyticsService: AnalyticsService,
    private val deviceInspector: DeviceInspector
) {

    fun notify(event: VenmoCheckoutEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(
            name = event.value,
            eventData = AnalyticsEventData(
                orderId = orderId?.takeIf { it.isNotEmpty() },
                appSwitchEnabled = true,
                venmoInstalled = deviceInspector.isVenmoInstalled.toString()
            )
        )
    }
}
