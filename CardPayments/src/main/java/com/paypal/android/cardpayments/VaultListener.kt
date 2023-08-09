package com.paypal.android.cardpayments

import androidx.annotation.MainThread
import com.paypal.android.corepayments.PayPalSDKError

/**
 * Listener to receive callbacks form [CardClient.vault].
 */
interface VaultListener {

    /**
     * Called when a successful vault has occurred.
     */
    @MainThread
    fun onVaultSuccess(result: VaultResult)

    /**
     * Called when a vault failure has occurred.
     */
    @MainThread
    fun onVaultFailure(error: PayPalSDKError)
}
