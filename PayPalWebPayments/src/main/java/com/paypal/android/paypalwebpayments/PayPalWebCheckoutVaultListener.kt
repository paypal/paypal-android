package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

/**
 * Implement this callback to receive results from [PayPalWebCheckoutClient].
 */
interface PayPalWebCheckoutVaultListener {

    /**
     * Called when the PayPal flow completes successfully.
     * @param result [PayPalWebCheckoutResult] with order information.
     */
    fun onPayPalWebVaultSuccess(result: PayPalWebCheckoutVaultResult)

    /**
     * Called when the PayPal flow completes with an error.
     * @param error [PayPalSDKError] explaining the reason for failure.
     */
    fun onPayPalWebVaultFailure(error: PayPalSDKError)

    /**
     * Called when the PayPal flow was canceled by the user.
     */
    fun onPayPalWebVaultCanceled()
}
