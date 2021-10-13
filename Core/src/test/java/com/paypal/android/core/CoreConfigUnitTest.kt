package com.paypal.android.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreConfigUnitTest {

    companion object {
        private const val CLIENT_ID = "sample-client-id"
        private const val CLIENT_SECRET = "sample-client-secret"
    }

    @Test
    fun `it should default to SANDBOX environment`() {
        val sut = CoreConfig(CLIENT_ID, CLIENT_SECRET)
        assertEquals(Environment.SANDBOX, sut.environment)
    }
}
