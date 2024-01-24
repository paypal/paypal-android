package com.paypal.android.paymentbuttons

import org.junit.Test

class PayPalButtonLabelTest {

    @Test
    fun `test invoke() with valid attributeIndex`() {
        val label = PayPalButtonLabel(1)
        assert(label == PayPalButtonLabel.CHECKOUT)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test invoke() with invalid attributeIndex`() {
        PayPalButtonLabel(100)
    }
}
