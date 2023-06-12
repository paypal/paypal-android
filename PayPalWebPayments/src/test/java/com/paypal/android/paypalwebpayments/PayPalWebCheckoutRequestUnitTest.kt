package com.paypal.android.paypalwebpayments

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalWebCheckoutRequestUnitTest {

    @Test
    fun `given an order id, PayPalRequest should return the same orderId`() {
        val orderId = "fake_order_id"
        val payPalRequest = PayPalWebCheckoutRequest(orderId)
        assertEquals(orderId, payPalRequest.orderId)
    }
}
