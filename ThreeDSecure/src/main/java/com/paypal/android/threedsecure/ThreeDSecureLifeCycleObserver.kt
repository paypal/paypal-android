package com.paypal.android.threedsecure

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

internal class ThreeDSecureLifeCycleObserver(private val threeDSecureClient: ThreeDSecureClient) :
    LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        threeDSecureClient.handleBrowserSwitchResult()
    }
}