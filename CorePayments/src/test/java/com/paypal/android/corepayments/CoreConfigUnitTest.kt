package com.paypal.android.corepayments

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreConfigUnitTest {

    @Test
    fun `it should default to SANDBOX environment`() {
        val sut = CoreConfig("fake-access-token")
        assertEquals(Environment.SANDBOX, sut.environment)
    }
}
