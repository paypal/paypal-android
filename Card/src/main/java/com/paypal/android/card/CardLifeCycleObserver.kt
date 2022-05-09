package com.paypal.android.card

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

internal class CardLifeCycleObserver(private val cardClient: CardClient) :
    LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        cardClient.handleBrowserSwitchResult()
    }
}