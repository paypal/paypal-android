package com.paypal.android.paypalwebpayments

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalWebCheckoutRequestUnitTest {

    @Test
    fun `given a ReturnToAppUrlConfig, PayPalWebCheckoutRequest should return the same config`() {
        val payPalURLConfig = PayPalURLConfig(
            returnAppUrl = "https://example.com/return",
            cancelAppUrl = "https://example.com/cancel",
            fallbackSchemeUrl = "com.example.app"
        )
        val request = PayPalWebCheckoutRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            payPalURLConfig = payPalURLConfig
        )
        assertEquals(payPalURLConfig, request.payPalURLConfig)
    }

    @Test
    fun `given no userAction, PayPalWebCheckoutRequest defaults to CONTINUE`() {
        val payPalURLConfig = PayPalURLConfig(
            returnAppUrl = "https://example.com/return",
            cancelAppUrl = "https://example.com/cancel",
            fallbackSchemeUrl = "com.example.app"
        )
        val request = PayPalWebCheckoutRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            payPalURLConfig = payPalURLConfig
        )
        assertEquals(PayPalUserAction.CONTINUE, request.userAction)
    }
}
