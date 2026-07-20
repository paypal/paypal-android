package com.paypal.android.paypalwebpayments.analytics

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
                endpoint = LatencyEndpoint.CREATE_ORDER,
                startTime = 1000L,
                endTime = 1500L
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
                flow = LatencyFlow.CHECKOUT,
                presentationType = PresentationType.APP_SWITCH,
                startTime = 2000L,
                endTime = 2200L
            )
        }
    }
}
