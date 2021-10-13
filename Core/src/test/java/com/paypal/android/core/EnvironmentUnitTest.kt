package com.paypal.android.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentUnitTest {

    @Test
    fun `it should return the correct url for the LIVE environment`() {
        assertEquals("https://api.paypal.com", Environment.LIVE.url)
    }

    @Test
    fun `it should return the correct url for the SANDBOX environment`() {
        assertEquals("https://api.sandbox.paypal.com", Environment.SANDBOX.url)
    }

    @Test
    fun `it should return the correct url for the STAGING environment`() {
        assertEquals("https://api.msmaster.qa.paypal.com", Environment.STAGING.url)
    }
}
