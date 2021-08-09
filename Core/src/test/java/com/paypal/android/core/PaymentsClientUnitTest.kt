package com.paypal.android.core

import org.junit.Assert.assertNotNull
import org.junit.Test

class PaymentsClientUnitTest {

    @Test
    fun constructor() {
        val sut = PaymentsClient()
        assertNotNull(sut)
    }
}
