package com.paypal.android.corepayments

sealed class ApproveGooglePayPaymentResult {
    data class Success(val status: String) : ApproveGooglePayPaymentResult()
    data class Failure(val error: PayPalSDKError) : ApproveGooglePayPaymentResult()
}
