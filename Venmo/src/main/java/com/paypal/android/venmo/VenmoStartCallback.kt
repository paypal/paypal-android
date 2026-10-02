package com.paypal.android.venmo

import androidx.annotation.MainThread

fun interface VenmoStartCallback {

    /**
     * Called when the result of a Venmo checkout start is available.
     */
    @MainThread
    fun onVenmoStartResult(result: VenmoStartResult)
}
