package com.paypal.android.card

import com.paypal.android.core.PaymentsConfiguration
import org.junit.Assert.assertNotNull
import org.junit.Test

class CardClientUnitTest {

    @Test
    fun constructor() {
        val sut = CardClient(PaymentsConfiguration("sample-client-id"))
        assertNotNull(sut)
    }
}
