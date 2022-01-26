package com.paypal.android.checkout

import com.paypal.android.checkout.paymentbutton.error.PayPalSDKError


sealed class PayPalCheckoutResult {
    class Success(val orderId: String?, val payerId: String?) : PayPalCheckoutResult()
    class Failure(val error: PayPalSDKError) : PayPalCheckoutResult()
    object Cancellation : PayPalCheckoutResult()
}
