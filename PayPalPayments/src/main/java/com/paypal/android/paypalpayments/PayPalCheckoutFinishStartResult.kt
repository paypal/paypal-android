package com.paypal.android.paypalpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalCheckoutFinishStartResult {
    class Success(val orderId: String?, val payerId: String?) : PayPalCheckoutFinishStartResult()
    class Failure(val error: PayPalSDKError, val orderId: String?) : PayPalCheckoutFinishStartResult()
    class Canceled(val orderId: String?) : PayPalCheckoutFinishStartResult()
    data object NoResult : PayPalCheckoutFinishStartResult()
}
