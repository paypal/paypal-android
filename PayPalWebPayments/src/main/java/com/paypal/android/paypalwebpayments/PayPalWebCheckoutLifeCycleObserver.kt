package com.paypal.android.paypalwebpayments

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

internal class PayPalWebCheckoutLifeCycleObserver(private val payPalClient: PayPalWebCheckoutClient) : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        payPalClient.handleBrowserSwitchResult()
    }
}
