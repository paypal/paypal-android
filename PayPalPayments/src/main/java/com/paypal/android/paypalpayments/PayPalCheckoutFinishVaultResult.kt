package com.paypal.android.paypalpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalCheckoutFinishVaultResult {

    class Success(val approvalSessionId: String) : PayPalCheckoutFinishVaultResult()
    class Failure(val error: PayPalSDKError) : PayPalCheckoutFinishVaultResult()
    data object Canceled : PayPalCheckoutFinishVaultResult()
    data object NoResult : PayPalCheckoutFinishVaultResult()
}
