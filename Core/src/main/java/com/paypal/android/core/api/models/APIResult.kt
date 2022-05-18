package com.paypal.android.core.api.models

sealed class APIResult<out T> {
    class Success<out T>(val data: T) : APIResult<T>()
    class Failure(
        val message: String? = null,
        val throwable: Throwable? = null
    ) : APIResult<Nothing>()
}
