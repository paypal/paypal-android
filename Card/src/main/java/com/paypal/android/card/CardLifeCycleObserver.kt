package com.paypal.android.card

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

internal class CardLifeCycleObserver(private val cardClient: CardClient) : DefaultLifecycleObserver {

    private var isFirstEventHandled = false

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // We want to ignore any previous onResume events before this class was instantiated
        if (isFirstEventHandled) {
            cardClient.handleBrowserSwitchResult()
        } else {
            isFirstEventHandled = true
        }
    }
}
