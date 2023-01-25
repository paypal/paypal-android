package com.paypal.android.webpayments

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class PayPalWebCheckoutLifeCycleObserver(private val payPalClient: PayPalWebCheckoutClient) : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        payPalClient.handleBrowserSwitchResult()
    }
}
