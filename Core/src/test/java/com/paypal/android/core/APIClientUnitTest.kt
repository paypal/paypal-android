package com.paypal.android.core

import org.junit.Assert.assertNotNull
import org.junit.Test

class APIClientUnitTest {

    @Test
    fun constructor() {
        val sut = APIClient(PaymentsConfiguration("sample-client-id"))
        assertNotNull(sut)
    }
}
