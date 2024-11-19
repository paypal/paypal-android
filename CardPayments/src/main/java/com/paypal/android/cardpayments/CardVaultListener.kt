package com.paypal.android.cardpayments

import androidx.annotation.MainThread
import com.paypal.android.corepayments.PayPalSDKError

/**
 * @suppress
 *
 * Listener to receive callbacks form [CardClient.vault].
 */
interface CardVaultListener {

    /**
     * Called when a successful vault has occurred.
     */
    @MainThread
    fun onVaultSuccess(result: CardVaultResult)

    /**
     * Called when authorization is required to continue.
     */
    @MainThread
    fun onVaultAuthorizationRequired(authChallenge: CardAuthChallenge)

    /**
     * Called when a vault failure has occurred.
     */
    @MainThread
    fun onVaultFailure(error: PayPalSDKError)
}
