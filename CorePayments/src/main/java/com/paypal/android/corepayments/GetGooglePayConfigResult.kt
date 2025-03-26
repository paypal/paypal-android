package com.paypal.android.corepayments

sealed class GetGooglePayConfigResult {
    data class Success(val value: GooglePayConfig) : GetGooglePayConfigResult()
    data class Failure(val error: PayPalSDKError) : GetGooglePayConfigResult()
}
