package com.paypal.android.venmo.analytics

import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.common.DeviceInspector
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class VenmoAnalyticsUnitTest {

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)
    private val deviceInspector = mockk<DeviceInspector> { every { isVenmoInstalled } returns true }
    private lateinit var venmoAnalytics: VenmoAnalytics

    @Before
    fun setUp() {
        venmoAnalytics = VenmoAnalytics(analyticsService, deviceInspector)
    }

    @Test
    fun notify_withStartEvent_sendsAnalyticsEvent() {
        val orderId = "order-123"

        venmoAnalytics.notify(VenmoCheckoutEvent.STARTED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:started",
                eventData = AnalyticsEventData(orderId = orderId, appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }

    @Test
    fun notify_withSuccessEvent_sendsAnalyticsEvent() {
        val orderId = "order-456"

        venmoAnalytics.notify(VenmoCheckoutEvent.SUCCEEDED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:succeeded",
                eventData = AnalyticsEventData(orderId = orderId, appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }

    @Test
    fun notify_withFailEvent_sendsAnalyticsEvent() {
        val orderId = "order-789"

        venmoAnalytics.notify(VenmoCheckoutEvent.FAILED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:failed",
                eventData = AnalyticsEventData(orderId = orderId, appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }

    @Test
    fun notify_withCanceledEvent_sendsAnalyticsEvent() {
        val orderId = "order-101"

        venmoAnalytics.notify(VenmoCheckoutEvent.CANCELED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:canceled",
                eventData = AnalyticsEventData(orderId = orderId, appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }

    @Test
    fun notify_withLaunchSuccessEvent_sendsAnalyticsEvent() {
        val orderId = "order-202"

        venmoAnalytics.notify(VenmoCheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:auth-challenge-presentation:succeeded",
                eventData = AnalyticsEventData(orderId = orderId, appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }

    @Test
    fun notify_withLaunchFailedEvent_sendsAnalyticsEvent() {
        val orderId = "order-303"

        venmoAnalytics.notify(VenmoCheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, orderId)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:auth-challenge-presentation:failed",
                eventData = AnalyticsEventData(orderId = orderId, appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }

    @Test
    fun notify_withNullOrderId_sendsAnalyticsEvent() {
        venmoAnalytics.notify(VenmoCheckoutEvent.STARTED, null)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:started",
                eventData = AnalyticsEventData(orderId = null, appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }

    @Test
    fun notify_alwaysSetsAppSwitchEnabledToTrue() {
        venmoAnalytics.notify(VenmoCheckoutEvent.SUCCEEDED, "order-123")

        verify {
            analyticsService.sendAnalyticsEvent(
                name = "venmo-payments:checkout:succeeded",
                eventData = AnalyticsEventData(orderId = "order-123", appSwitchEnabled = true, venmoInstalled = "true")
            )
        }
    }
}
