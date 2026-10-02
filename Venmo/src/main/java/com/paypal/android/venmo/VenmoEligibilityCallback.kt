package com.paypal.android.venmo

import androidx.annotation.MainThread

fun interface VenmoEligibilityCallback {

    /**
     * Called when the result of a Venmo eligibility check is available.
     */
    @MainThread
    fun onVenmoEligibilityResult(result: VenmoEligibilityResult)
}
