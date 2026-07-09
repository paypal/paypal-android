package com.paypal.android.venmo

import com.paypal.android.corepayments.PayPalSDKError

sealed class VenmoFinishStartResult() {
    class Success(
        val token: String,
        val payerId: String,
        val approved: Boolean
    ) : VenmoFinishStartResult()

    class Failure(val error: PayPalSDKError) : VenmoFinishStartResult()

    //    class Canceled(val orderId: String?) : PayPalWebCheckoutFinishStartResult()
//    data object NoResult : VenmoFinishStartResult()
}

