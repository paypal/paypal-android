package com.paypal.android.checkout

import com.paypal.android.core.CoreSDKError

sealed class PayPalCheckoutResult {
    class Success(val orderId: String?, val payerId: String?) : PayPalCheckoutResult()
    class Failure(val error: CoreSDKError) : PayPalCheckoutResult()
    object Cancellation : PayPalCheckoutResult()
}
