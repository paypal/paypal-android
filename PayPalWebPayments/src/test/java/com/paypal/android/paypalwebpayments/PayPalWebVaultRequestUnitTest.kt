package com.paypal.android.paypalwebpayments

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalWebVaultRequestUnitTest {

    private val fakeReturnToAppUrlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "https://example.com/return",
        cancelAppUrl = "https://example.com/cancel",
        fallbackSchemeUrl = "com.example.app://paypal-sdk/paypal-checkout"
    )

    @Test
    fun `userIdentity is stored correctly`() {
        val request = PayPalWebVaultRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = fakeReturnToAppUrlConfig
        )
        assertEquals(PayPalUserIdentity.Unknown, request.userIdentity)
    }

    @Test
    fun `returnToAppUrlConfig is stored correctly`() {
        val request = PayPalWebVaultRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = fakeReturnToAppUrlConfig
        )
        assertEquals(fakeReturnToAppUrlConfig, request.returnToAppUrlConfig)
    }

    @Test
    fun `userAction defaults to CONTINUE`() {
        val request = PayPalWebVaultRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = fakeReturnToAppUrlConfig
        )
        assertEquals(PayPalUserAction.CONTINUE, request.userAction)
    }

    @Test
    fun `userAction can be overridden to SETUP_NOW`() {
        val request = PayPalWebVaultRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = fakeReturnToAppUrlConfig,
            userAction = PayPalUserAction.SETUP_NOW
        )
        assertEquals(PayPalUserAction.SETUP_NOW, request.userAction)
    }

    @Test
    fun `Email userIdentity stores email address`() {
        val request = PayPalWebVaultRequest(
            userIdentity = PayPalUserIdentity.Email("buyer@example.com"),
            returnToAppUrlConfig = fakeReturnToAppUrlConfig
        )
        assertEquals("buyer@example.com", (request.userIdentity as PayPalUserIdentity.Email).email)
    }
}
