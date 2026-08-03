package com.paypal.android.paypalpayments

import androidx.annotation.MainThread

fun interface PayPalStartCallback {

    /**
     * Called when the result of a PayPal web launch is available.
     */
    @MainThread
    fun onPayPalStartResult(result: PayPalPresentAuthChallengeResult)
}
