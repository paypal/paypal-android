package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebCheckoutVaultResult {

    data class DidLaunchAuth(val authState: String) : PayPalWebCheckoutVaultResult()

    data class Failure(val error: PayPalSDKError) : PayPalWebCheckoutVaultResult()
}
