package com.paypal.android.paymentbuttons.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PaymentButtonAnalyticsTest {

    private val analyticsService = mockk<AnalyticsService>(relaxed = true)
    private val sut = PaymentButtonAnalytics(analyticsService)

    @Test
    fun `notify sends INITIALIZED event with correct button type`() {
        sut.notify(PaymentButtonEvent.INITIALIZED, "PayPal")

        verify(exactly = 1) {
            analyticsService.sendAnalyticsEvent(
                "payment-button:initialized",
                buttonType = "PayPal"
            )
        }
    }

    @Test
    fun `notify sends TAPPED event with correct button type`() {
        sut.notify(PaymentButtonEvent.TAPPED, "Pay Later")

        verify(exactly = 1) {
            analyticsService.sendAnalyticsEvent(
                "payment-button:tapped",
                buttonType = "Pay Later"
            )
        }
    }
}
