package com.paypal.android.paypalpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalFinishVaultResult {

    class Success(val approvalSessionId: String) : PayPalFinishVaultResult()
    class Failure(val error: PayPalSDKError) : PayPalFinishVaultResult()
    data object Canceled : PayPalFinishVaultResult()
    data object NoResult : PayPalFinishVaultResult()
}
