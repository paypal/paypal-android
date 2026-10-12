package com.paypal.android.venmo

import androidx.annotation.MainThread

fun interface VenmoResultCallback {

    /**
     * Called when the result of presenting a Venmo auth challenge (checkout or vault) is available.
     */
    @MainThread
    fun onVenmoResult(result: VenmoPresentAuthChallengeResult)
}
