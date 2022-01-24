package com.paypal.android.checkout


sealed class PayPalCheckoutResult {
    class Success(val orderId: String?, val payerId: String?) : PayPalCheckoutResult()
    // TODO: Create error object for this
    class Failure(val error: Error) : PayPalCheckoutResult()
    object Cancellation : PayPalCheckoutResult()
}
