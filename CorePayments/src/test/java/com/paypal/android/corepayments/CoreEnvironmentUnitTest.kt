package com.paypal.android.corepayments

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreEnvironmentUnitTest {

    @Test
    fun `it should return the correct url for the LIVE environment`() {
        assertEquals("https://api-m.paypal.com", CoreEnvironment.LIVE.url)
    }

    @Test
    fun `it should return the correct url for the SANDBOX environment`() {
        assertEquals("https://api-m.sandbox.paypal.com", CoreEnvironment.SANDBOX.url)
    }
}
