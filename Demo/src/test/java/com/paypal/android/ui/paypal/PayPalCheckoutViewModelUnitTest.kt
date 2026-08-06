package com.paypal.android.ui.paypal

import com.paypal.android.paypalpayments.PayPalUserAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayPalCheckoutViewModelUnitTest {

    @Test
    fun `maps checkout user actions to valid Orders V2 values`() {
        assertEquals("PAY_NOW", PayPalUserAction.PAY_NOW.toOrderUserAction())
        assertEquals("CONTINUE", PayPalUserAction.CONTINUE.toOrderUserAction())
        assertNull(PayPalUserAction.SETUP_NOW.toOrderUserAction())
    }
}
