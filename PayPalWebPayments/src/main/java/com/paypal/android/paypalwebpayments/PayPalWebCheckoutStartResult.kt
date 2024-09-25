package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebCheckoutStartResult {

    object DidLaunchAuth : PayPalWebCheckoutStartResult()

    data class Failure(val error: PayPalSDKError) : PayPalWebCheckoutStartResult()
}
