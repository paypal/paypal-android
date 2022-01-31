package com.paypal.android.checkout

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PayPalLifeCycleObserverUnitTest {

    @Test
    fun `when resume is called, payPalClient handles browser switch result`() {
        val payPalClient = mockk<PayPalClient>(relaxed = true)

        val lifeCycleObserver = PayPalLifeCycleObserver(payPalClient)
        lifeCycleObserver.resume()

        verify { payPalClient.handleBrowserSwitchResult() }
    }
}