package com.paypal.android.corepayments

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentUnitTest {

    @Test
    fun `it should return the correct url for the LIVE environment`() {
        assertEquals("https://api-m.paypal.com", Environment.LIVE.url)
    }

    @Test
    fun `it should return the correct url for the SANDBOX environment`() {
        assertEquals("https://api-m.sandbox.paypal.com", Environment.SANDBOX.url)
    }
}
