package com.paypal.android.paypalwebpayments.analytics

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
class PayPalWebAnalyticsUnitTest {

    private lateinit var analyticsService: AnalyticsService
    private lateinit var sut: PayPalWebAnalytics

    @Before
    fun beforeEach() {
        analyticsService = mockk(relaxed = true)
        sut = PayPalWebAnalytics(analyticsService)
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
        val params = AppSwitchAnalyticsEventParams(
            checkoutOrderId = "fake-order-id",
            appSwitchEnabled = true,
            linkType = LinkType.APP_LINK,
        )

        sut.notify(CheckoutEvent.APP_SWITCH_STARTED, params = params)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = CheckoutEvent.APP_SWITCH_STARTED.value,
                eventData = match { it.linkType == "applink" && it.orderId == "fake-order-id" },
            )
        }
    }

    @Test
    fun `notify forwards linkType to the analytics service for vault events`() {
        val params = AppSwitchAnalyticsEventParams(
            vaultSetupTokenId = "fake-setup-token-id",
            appSwitchEnabled = false,
            linkType = LinkType.DEEP_LINK,
        )

        sut.notify(VaultEvent.AUTH_CHALLENGE_PRESENTATION_STARTED, params = params)

        verify {
            analyticsService.sendAnalyticsEvent(
                name = VaultEvent.AUTH_CHALLENGE_PRESENTATION_STARTED.value,
                eventData = match { it.linkType == "deeplink" && it.orderId == "fake-setup-token-id" },
            )
        }
    }
}
