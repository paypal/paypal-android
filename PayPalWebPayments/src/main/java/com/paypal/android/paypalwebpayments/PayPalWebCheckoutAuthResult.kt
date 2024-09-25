package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebCheckoutAuthResult {

    data class Success(val orderId: String?, val payerId: String?) : PayPalWebCheckoutAuthResult()

    data class Failure(val error: PayPalSDKError, val orderId: String? = null) :
        PayPalWebCheckoutAuthResult()

    object Canceled : PayPalWebCheckoutAuthResult()

    object NoResult : PayPalWebCheckoutAuthResult()
}
