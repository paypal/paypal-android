package com.paypal.android.checkoutweb

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PayPalLifeCycleObserverUnitTest {

    @Test
    fun `when resume is called, payPalClient handles browser switch result`() {
        val payPalClient = mockk<PayPalWebClient>(relaxed = true)

        val lifeCycleObserver = PayPalLifeCycleObserver(payPalClient)
        lifeCycleObserver.resume()

        verify { payPalClient.handleBrowserSwitchResult() }
    }
}
