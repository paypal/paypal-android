package com.paypal.android.venmo.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class VenmoAnalyticsUnitTest {

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)
    private lateinit var venmoAnalytics: VenmoAnalytics

    @Before
    fun setUp() {
        venmoAnalytics = VenmoAnalytics(analyticsService)
    }

    @Test
    fun notify_withStartEvent_sendsAnalyticsEvent() {
        val orderId = "order-123"

        venmoAnalytics.notify(VenmoCheckoutEvent.START, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:start",
                orderId = orderId,
                appSwitchEnabled = true
            )
        }
    }

    @Test
    fun notify_withSuccessEvent_sendsAnalyticsEvent() {
        val orderId = "order-456"

        venmoAnalytics.notify(VenmoCheckoutEvent.SUCCESS, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:success",
                orderId = orderId,
                appSwitchEnabled = true
            )
        }
    }

    @Test
    fun notify_withFailEvent_sendsAnalyticsEvent() {
        val orderId = "order-789"

        venmoAnalytics.notify(VenmoCheckoutEvent.FAIL, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:fail",
                orderId = orderId,
                appSwitchEnabled = true
            )
        }
    }

    @Test
    fun notify_withCanceledEvent_sendsAnalyticsEvent() {
        val orderId = "order-101"

        venmoAnalytics.notify(VenmoCheckoutEvent.CANCELED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:canceled",
                orderId = orderId,
                appSwitchEnabled = true
            )
        }
    }

    @Test
    fun notify_withLaunchSuccessEvent_sendsAnalyticsEvent() {
        val orderId = "order-202"

        venmoAnalytics.notify(VenmoCheckoutEvent.LAUNCH_SUCCESS, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:launched:success",
                orderId = orderId,
                appSwitchEnabled = true
            )
        }
    }

    @Test
    fun notify_withLaunchFailedEvent_sendsAnalyticsEvent() {
        val orderId = "order-303"

        venmoAnalytics.notify(VenmoCheckoutEvent.LAUNCH_FAILED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:launched:failed",
                orderId = orderId,
                appSwitchEnabled = true
            )
        }
    }

    @Test
    fun notify_withNullOrderId_sendsAnalyticsEvent() {
        venmoAnalytics.notify(VenmoCheckoutEvent.START, null)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:start",
                orderId = null,
                appSwitchEnabled = true
            )
        }
    }

    @Test
    fun notify_alwaysSetsAppSwitchEnabledToTrue() {
        venmoAnalytics.notify(VenmoCheckoutEvent.SUCCESS, "order-123")

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo:checkout:success",
                orderId = "order-123",
                appSwitchEnabled = true
            )
        }
    }
}
