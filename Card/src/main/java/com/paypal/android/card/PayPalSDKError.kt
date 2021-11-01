package com.paypal.android.card

data class PayPalSDKError(
    val code: Int?,
    val errorDescription: String?
) : Exception("Error: $code - Description: $errorDescription")
