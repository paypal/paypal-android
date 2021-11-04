package com.paypal.android.core

data class PayPalSDKError(
    val code: Int?,
    val errorDescription: String?
) : Exception("Error: $code - Description: $errorDescription")
