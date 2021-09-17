package com.paypal.android.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentsConfigurationUnitTest {

    @Test
    fun `it should default to SANDBOX environment`() {
        val sut = PaymentsConfiguration(clientId = "123")
        assertEquals(Environment.SANDBOX, sut.environment)
    }
}
