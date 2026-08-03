package com.paypal.android.paypalpayments

import androidx.annotation.MainThread

fun interface PayPalVaultCallback {
    /**
     * Called when the PayPal web vault operation completes.
     *
     * @param result [PayPalPresentAuthChallengeResult] result with details
     */
    @MainThread
    fun onPayPalVaultResult(result: PayPalPresentAuthChallengeResult)
}
