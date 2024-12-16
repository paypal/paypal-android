package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebCheckoutFinishVaultResult {

    class Success(val approvalSessionId: String) : PayPalWebCheckoutFinishVaultResult()
    class Failure(val error: PayPalSDKError) : PayPalWebCheckoutFinishVaultResult()
    data object Canceled : PayPalWebCheckoutFinishVaultResult()
    data object NoResult : PayPalWebCheckoutFinishVaultResult()
}
