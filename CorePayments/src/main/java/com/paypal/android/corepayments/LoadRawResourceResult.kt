package com.paypal.android.corepayments

sealed class LoadRawResourceResult {
    data class Success(val value: String) : LoadRawResourceResult()
    data class Failure(val error: PayPalSDKError) : LoadRawResourceResult()
}
