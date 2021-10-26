package com.paypal.android.card

data class PayPalSDKError(
    val code: Int?,
    val domain: String?,
    val errorDescription: String?
): Exception("Error: $code - Domain: $domain - Description: $errorDescription")
