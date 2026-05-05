package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class SDKResult<out T> {
    data class Success<T>(val value: T) : SDKResult<T>()
    data class Failure(val error: PayPalSDKError) : SDKResult<Nothing>()
}
