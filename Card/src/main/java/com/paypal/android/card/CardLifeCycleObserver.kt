package com.paypal.android.card

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

internal class CardLifeCycleObserver(private val cardClient: CardClient) : DefaultLifecycleObserver {

    //private var isFirstEventHandled = false

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // We want to ignore any previous onResume events before this class was instantiated
        //if (isFirstEventHandled) {\
        Log.e("LIFECYCLE OBSERVER", "ON RESUME")
            cardClient.handleBrowserSwitchResult()
//        } else {
//            isFirstEventHandled = true
//        }
    }
}
