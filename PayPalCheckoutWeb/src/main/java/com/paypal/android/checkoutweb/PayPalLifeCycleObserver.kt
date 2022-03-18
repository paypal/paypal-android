package com.paypal.android.checkoutweb

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class PayPalLifeCycleObserver(private val payPalClient: PayPalWebClient) : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        payPalClient.handleBrowserSwitchResult()
    }
}
