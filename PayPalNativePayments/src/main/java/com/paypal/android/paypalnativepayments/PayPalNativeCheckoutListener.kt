package com.paypal.android.paypalnativepayments

import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData

/**
 * Implement this callback to receive results from [PayPalNativeCheckoutClient].
 */
interface PayPalNativeCheckoutListener {

    /**
     * Called when the PayPal flow is about to start.
     */
    fun onPayPalCheckoutStart()

    /**
     * Called when the PayPal flow completes successfully.
     * @param result [PayPalNativeCheckoutResult] with order information.
     */
    fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult)

    /**
     * Called when the PayPal flow completes with an error.
     * @param error [PayPalSDKError] explaining the reason for failure.
     */
    fun onPayPalCheckoutFailure(error: PayPalSDKError)

    /**
     * Called when the PayPal flow was canceled by the user.
     */
    fun onPayPalCheckoutCanceled()
}
