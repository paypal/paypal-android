package com.paypal.android.checkoutweb

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class PayPalWebLifeCycleObserver(private val payPalClient: PayPalWebCheckoutClient) : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        payPalClient.handleBrowserSwitchResult()
    }
}
