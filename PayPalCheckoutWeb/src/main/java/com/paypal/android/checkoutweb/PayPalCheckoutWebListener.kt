package com.paypal.android.checkoutweb

import com.paypal.android.core.PayPalSDKError

/**
 * Implement this callback to receive results from [PayPalWebClient].
 */
interface PayPalCheckoutWebListener {

    /**
     * Called when the PayPal flow completes successfully.
     * @param result [PayPalCheckoutWebResult] with order information.
     */
    fun onPayPalWebSuccess(result: PayPalCheckoutWebResult)

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
