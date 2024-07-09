package com.paypal.android.venmopayments

import org.junit.Assert.assertNotNull
import org.junit.Test

class VenmoClientUnitTest {

    @Test
    fun exists() {
        val sut = VenmoClient()
        assertNotNull(sut)
    }
}
