package com.paypal.android.corepayments

sealed class CoreSDKResult<out S> {
    data class Success<S>(val value: S) : CoreSDKResult<S>()
    data class Failure(val value: PayPalSDKError) : CoreSDKResult<Nothing>()
}
