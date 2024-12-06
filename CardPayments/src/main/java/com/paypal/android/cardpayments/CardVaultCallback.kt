package com.paypal.android.cardpayments

import androidx.annotation.MainThread

fun interface CardVaultCallback {

    /**
     * Called when a successful vault has occurred.
     */
    @MainThread
    fun onCardVaultResult(result: CardVaultResult)
}
