package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalWebVaultAuthResult {
    data class Success(val approvalSessionId: String) : PayPalWebVaultAuthResult()

    data class Failure(val error: PayPalSDKError) : PayPalWebVaultAuthResult()

    object Canceled : PayPalWebVaultAuthResult()

    object NoResult : PayPalWebVaultAuthResult()
}