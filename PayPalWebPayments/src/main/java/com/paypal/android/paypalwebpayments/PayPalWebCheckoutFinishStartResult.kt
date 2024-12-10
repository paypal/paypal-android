package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebCheckoutFinishStartResult {
    class Success(val orderId: String?, val payerId: String?) : PayPalWebCheckoutFinishStartResult()
    class Failure(val error: PayPalSDKError, val orderId: String?) : PayPalWebCheckoutFinishStartResult()
    class Canceled(val orderId: String?) : PayPalWebCheckoutFinishStartResult()
    data object NoResult : PayPalWebCheckoutFinishStartResult()
}
