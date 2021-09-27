package com.paypal.android.card

import com.paypal.android.core.PaymentsConfiguration
import org.junit.Assert.assertNotNull
import org.junit.Test

class CardClientUnitTest {

    companion object {
        private const val CLIENT_ID = "sample-client-id"
        private const val CLIENT_SECRET = "sample-client-secret"
    }

    @Test
    fun constructor() {
        val sut = CardClient(PaymentsConfiguration(CLIENT_ID, CLIENT_SECRET))
        assertNotNull(sut)
    }
}
