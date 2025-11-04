package com.paypal.android.paypalwebpayments

import androidx.annotation.MainThread

fun interface PayPalWebVaultCallback {
    /**
     * Called when the PayPal web vault operation completes.
     *
     * @param result [PayPalPresentAuthChallengeResult] result with details
     */
    @MainThread
    fun onPayPalWebVaultResult(result: PayPalPresentAuthChallengeResult)
}
