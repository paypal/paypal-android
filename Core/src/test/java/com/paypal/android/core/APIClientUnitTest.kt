package com.paypal.android.core

import org.junit.Assert.assertNotNull
import org.junit.Test

class APIClientUnitTest {

    companion object {
        private const val CLIENT_ID = "sample-client-id"
        private const val CLIENT_SECRET = "sample-client-secret"
    }

    @Test
    fun constructor() {
        val sut = APIClient(PaymentsConfiguration(CLIENT_ID, CLIENT_SECRET))
        assertNotNull(sut)
    }
}
