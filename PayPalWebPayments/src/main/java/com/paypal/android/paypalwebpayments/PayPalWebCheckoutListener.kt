package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

/**
 * Implement this callback to receive results from [PayPalWebCheckoutClient.start].
 */
interface PayPalWebCheckoutListener {

    /**
     * Called when the PayPal flow completes successfully.
     * @param result [PayPalWebCheckoutResult] with order information.
     */
    fun onPayPalWebSuccess(result: PayPalWebCheckoutResult)

    /**
     * Called when the PayPal flow completes with an error.
     * @param error [PayPalSDKError] explaining the reason for failure.
     */
    fun onPayPalWebFailure(error: PayPalSDKError)

    /**
     * Called when the PayPal flow was canceled by the user.
     */
    fun onPayPalWebCanceled()
}
