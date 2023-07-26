package com.paypal.android.cardpayments

import androidx.annotation.MainThread

/**
 * Listener to receive vault results.
 */
interface VaultListener {

    /**
     * Called after a successful vault occurs.
     */
    @MainThread
    fun onVaultSuccess(result: VaultResult)
}
