package com.paypal.android.paypalpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalFinishStartResult {
    class Success(val orderId: String?, val payerId: String?) : PayPalFinishStartResult()
    class Failure(val error: PayPalSDKError, val orderId: String?) : PayPalFinishStartResult()
    class Canceled(val orderId: String?) : PayPalFinishStartResult()
    data object NoResult : PayPalFinishStartResult()
}
