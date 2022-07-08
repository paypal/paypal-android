package com.paypal.android.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreConfigUnitTest {

    @Test
    fun `it should default to SANDBOX environment`() {
        val sut = CoreConfig()
        assertEquals(Environment.SANDBOX, sut.environment)
    }
}
