package com.paypal.android.webpayments

/**
 * A result passed to a [PayPalWebCheckoutListener] when the PayPal flow completes successfully.
 */
data class PayPalWebCheckoutResult(val orderId: String?, val payerId: String?)
