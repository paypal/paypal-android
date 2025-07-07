package com.paypal.android.paypalwebpayments

import androidx.annotation.MainThread

fun interface PayPalWebCheckoutCallback {
    /**
     * Called when the PayPal web checkout operation completes.
     *
     * @param result [PayPalPresentAuthChallengeResult] result with details
     */
    @MainThread
    fun onPayPalWebCheckoutResult(result: PayPalPresentAuthChallengeResult)
}
