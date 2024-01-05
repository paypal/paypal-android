package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

/**
 * Implement this callback to receive results from [PayPalWebCheckoutClient.vault].
 */
interface PayPalWebVaultListener {

    /**
     * Called when vaulting a PayPal payment method completes successfully.
     * @param result [PayPalWebVaultResult] with order information.
     */
    fun onPayPalWebVaultSuccess(result: PayPalWebVaultResult)

    /**
     * Called when vaulting a PayPal payment method completes with an error.
     * @param error [PayPalSDKError] explaining the reason for failure.
     */
    fun onPayPalWebVaultFailure(error: PayPalSDKError)

    /**
     * Called when a user cancels PayPal payment method vaulting.
     */
    fun onPayPalWebVaultCanceled()
}
