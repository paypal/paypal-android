package com.paypal.android.checkout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class PayPalLifeCycleObserver(private val payPalClient: PayPalClient) : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        payPalClient.handleBrowserSwitchResult()
    }
}
