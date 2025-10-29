package com.paypal.android.paypalwebpayments

import androidx.annotation.MainThread

fun interface PayPalWebVaultCallback {

    /**
     * Called when the result of a PayPal web vault launch is available.
     */
    @MainThread
    fun onPayPalWebVaultResult(result: PayPalPresentAuthChallengeResult)
}
