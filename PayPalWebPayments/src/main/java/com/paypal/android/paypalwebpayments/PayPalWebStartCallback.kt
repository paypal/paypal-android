package com.paypal.android.paypalwebpayments

import androidx.annotation.MainThread

fun interface PayPalWebStartCallback {

    /**
     * Called when the result of a PayPal web launch is available.
     */
    @MainThread
    fun onPayPalWebStartResult(result: PayPalPresentAuthChallengeResult)
}