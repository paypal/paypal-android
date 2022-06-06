package com.paypal.android.card

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

        activity?.let { cardClient.handleBrowserSwitchResult(it) }
    }
}
