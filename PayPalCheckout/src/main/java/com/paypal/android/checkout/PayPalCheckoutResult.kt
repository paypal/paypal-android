package com.paypal.android.checkout

import com.paypal.android.checkout.pojo.ErrorInfo

sealed class PayPalCheckoutResult {
    class Success(val orderId: String?, val payerId: String?) : PayPalCheckoutResult()
    class Failure(val error: ErrorInfo) : PayPalCheckoutResult()
}
