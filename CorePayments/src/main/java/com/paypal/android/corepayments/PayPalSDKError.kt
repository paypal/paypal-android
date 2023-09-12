package com.paypal.android.corepayments

/**
 * Class used to describe PayPal errors when they occur.
 */
class PayPalSDKError(
    val code: Int,
    val errorDescription: String,
    val correlationId: String? = null,
    reason: Exception? = null
) : Exception("Error: $code - Description: $errorDescription", reason)
