package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

/**
 * A result passed to a [PayPalWebCheckoutListener] when the PayPal flow completes successfully.
 */
sealed class PayPalWebCheckoutResult {

    class Success(val orderId: String?, val payerId: String?) : PayPalWebCheckoutResult()

    data class Failure(val error: PayPalSDKError) : PayPalWebCheckoutResult()

    object Canceled : PayPalWebCheckoutResult()
}
