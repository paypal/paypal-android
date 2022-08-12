package com.paypal.android.core

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class CoreCoroutineExceptionHandler(private val handler: (PayPalSDKError) -> Unit) :
    AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        val error = when (exception) {
            is PayPalSDKError -> exception
            else -> {
                val message = exception.localizedMessage ?: "Something went wrong"
                PayPalSDKError(0, message)
            }
        }
        handler(error)
    }
}
