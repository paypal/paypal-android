package com.paypal.android.checkout

/**
 * A result passed to a [PayPalListener] when the PayPal flow completes successfully.
 */
data class PayPalCheckoutResult(val orderId: String?, val payerId: String?)
