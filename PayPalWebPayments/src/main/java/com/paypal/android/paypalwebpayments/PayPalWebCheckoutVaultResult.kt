package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebCheckoutVaultResult {

    object DidLaunchAuth : PayPalWebCheckoutVaultResult()

    data class Failure(val error: PayPalSDKError) : PayPalWebCheckoutVaultResult()
}
