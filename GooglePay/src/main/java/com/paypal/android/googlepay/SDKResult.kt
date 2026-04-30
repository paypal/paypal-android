package com.paypal.android.googlepay

import com.paypal.android.corepayments.PayPalSDKError

// TODO: move to core module
sealed class SDKResult<out T> {
    data class Success<T>(val value: T) : SDKResult<T>()
    data class Failure(val error: PayPalSDKError) : SDKResult<Nothing>()
}