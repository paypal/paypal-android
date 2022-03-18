package com.paypal.android.checkoutweb

/**
 * A result passed to a [PayPalCheckoutWebListener] when the PayPal flow completes successfully.
 */
data class PayPalCheckoutWebResult(val orderId: String?, val payerId: String?)
