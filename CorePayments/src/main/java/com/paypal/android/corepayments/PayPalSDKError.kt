package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * Class used to describe PayPal errors when they occur.
 */
class PayPalSDKError @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    val code: Int,
    val errorDescription: String,
    val correlationId: String? = null,
    reason: Throwable? = null
) : Exception("Error: $code - Description: $errorDescription", reason)
