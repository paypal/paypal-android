package com.paypal.android.card

import com.paypal.android.core.APIClient
import org.junit.Assert.assertNotNull
import org.junit.Test

class CardClientUnitTest {

    @Test
    fun constructor() {
        val sut = CardClient(APIClient())
        assertNotNull(sut)
    }
}
