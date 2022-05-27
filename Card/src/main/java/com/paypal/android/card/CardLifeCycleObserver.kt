package com.paypal.android.card

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

internal class CardLifeCycleObserver(private val cardClient: CardClient) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        val activity = when (owner) {
            is FragmentActivity -> owner
            is Fragment -> owner.requireActivity()
            else -> null
        }

        Log.e("LIFECYCLE OBSERVER", "ON RESUME")
        activity?.let { cardClient.handleBrowserSwitchResult(it) }
    }
}
