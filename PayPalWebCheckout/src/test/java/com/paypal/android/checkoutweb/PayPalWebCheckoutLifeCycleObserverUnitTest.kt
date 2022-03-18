package com.paypal.android.checkoutweb

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PayPalWebCheckoutLifeCycleObserverUnitTest {

    @Test
    fun `when resume is called, payPalClient handles browser switch result`() {
        val payPalClient = mockk<PayPalWebCheckoutClient>(relaxed = true)

        val lifeCycleObserver = PayPalWebCheckoutLifeCycleObserver(payPalClient)
        lifeCycleObserver.resume()

        verify { payPalClient.handleBrowserSwitchResult() }
    }
}
