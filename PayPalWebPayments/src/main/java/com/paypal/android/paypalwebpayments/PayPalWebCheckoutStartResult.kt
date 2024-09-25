package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebCheckoutStartResult {

    data class DidLaunchAuth(val authState: String) : PayPalWebCheckoutStartResult()

    data class Failure(val error: PayPalSDKError) : PayPalWebCheckoutStartResult()
}
