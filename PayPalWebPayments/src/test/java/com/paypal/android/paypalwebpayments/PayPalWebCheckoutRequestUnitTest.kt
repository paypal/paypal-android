package com.paypal.android.paypalwebpayments

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalWebCheckoutRequestUnitTest {

    @Test
    fun `given a ReturnToAppUrlConfig, PayPalWebCheckoutRequest should return the same config`() {
        val returnToAppUrlConfig = ReturnToAppUrlConfig(
            returnAppUrl = "https://example.com/return",
            cancelAppUrl = "https://example.com/cancel",
            fallbackSchemeUrl = "com.example.app"
        )
        val request = PayPalWebCheckoutRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = returnToAppUrlConfig
        )
        assertEquals(returnToAppUrlConfig, request.returnToAppUrlConfig)
    }

    @Test
    fun `given no userAction, PayPalWebCheckoutRequest defaults to CONTINUE`() {
        val returnToAppUrlConfig = ReturnToAppUrlConfig(
            returnAppUrl = "https://example.com/return",
            cancelAppUrl = "https://example.com/cancel",
            fallbackSchemeUrl = "com.example.app"
        )
        val request = PayPalWebCheckoutRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = returnToAppUrlConfig
        )
        assertEquals(PayPalUserAction.CONTINUE, request.userAction)
    }
}
