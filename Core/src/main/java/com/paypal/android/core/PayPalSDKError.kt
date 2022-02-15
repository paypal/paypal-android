package com.paypal.android.core

data class PayPalSDKError(
    val code: Int,
    val errorDescription: String,
    val correlationID: String? = null
) : Exception("Error: $code - Description: $errorDescription")
