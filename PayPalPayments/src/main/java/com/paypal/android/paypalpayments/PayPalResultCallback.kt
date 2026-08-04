package com.paypal.android.paypalpayments

import androidx.annotation.MainThread

fun interface PayPalResultCallback {

    /**
     * Called when the PayPal start or vault operation completes.
     *
     * @param result [PayPalPresentAuthChallengeResult] result with details
     */
    @MainThread
    fun onPayPalResult(result: PayPalPresentAuthChallengeResult)
}
