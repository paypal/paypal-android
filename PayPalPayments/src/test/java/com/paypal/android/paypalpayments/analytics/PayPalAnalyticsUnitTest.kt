package com.paypal.android.paypalpayments.analytics

import com.paypal.android.corepayments.LinkType
import com.paypal.android.corepayments.analytics.AnalyticsEventData
import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalAnalyticsUnitTest {

    private lateinit var analyticsService: AnalyticsService
    private lateinit var sut: PayPalAnalytics

    @Before
    fun beforeEach() {
        analyticsService = mockk(relaxed = true)
        sut = PayPalAnalytics(analyticsService)
    }

    @Test
    fun `notifyApiRequestLatency sends api-request-latency event with endpoint and timing`() {
        sut.notifyApiRequestLatency(
            endpoint = LatencyEndpoint.CREATE_ORDER,
            startTime = 1000L,
            endTime = 1500L
        )

        verify {
            analyticsService.sendAnalyticsEvent(
                name = LatencyEvent.API_REQUEST_LATENCY.value,
                eventData = AnalyticsEventData(
                    endpoint = LatencyEndpoint.CREATE_ORDER,
                    startTime = 1000L,
                    endTime = 1500L
                )
            )
        }
    }

    @Test
    fun `notifyUserPerceivedLatency sends user-perceived-latency event with flow and presentation type`() {
        sut.notifyUserPerceivedLatency(
            flow = LatencyFlow.CHECKOUT,
            presentationType = PresentationType.APP_SWITCH,
            startTime = 2000L,
            endTime = 2200L
        )

        verify {
            analyticsService.sendAnalyticsEvent(
                name = LatencyEvent.USER_PERCEIVED_LATENCY.value,
                eventData = AnalyticsEventData(
                    flow = LatencyFlow.CHECKOUT,
                    presentationType = PresentationType.APP_SWITCH,
                    startTime = 2000L,
                    endTime = 2200L
                )
            )
        }
    }

    @Test
    fun `notify forwards linkType to the analytics service for checkout events`() {
        val params = AnalyticsEventParams(
            orderIdOrSetupTokenId = "fake-order-id",
            appSwitchEnabled = true,
            linkType = LinkType.APP_LINK,
        )

        sut.notify(PayPalEvent.APP_SWITCH_STARTED, params = params)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = PayPalEvent.APP_SWITCH_STARTED.value,
                eventData = match { it.linkType == "universal" && it.orderId == "fake-order-id" },
            )
        }
    }

    @Test
    fun `notify forwards linkType to the analytics service for vault events`() {
        val params = AnalyticsEventParams(
            orderIdOrSetupTokenId = "fake-setup-token-id",
            appSwitchEnabled = false,
            linkType = LinkType.DEEP_LINK,
        )

        sut.notify(PayPalEvent.AUTH_CHALLENGE_PRESENTATION_STARTED, params = params)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = PayPalEvent.AUTH_CHALLENGE_PRESENTATION_STARTED.value,
                eventData = match { it.linkType == "deeplink" && it.orderId == "fake-setup-token-id" },
            )
        }
    }
}
