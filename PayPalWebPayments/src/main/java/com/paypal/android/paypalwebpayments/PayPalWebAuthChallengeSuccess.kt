package com.paypal.android.paypalwebpayments

internal data class PayPalWebAuthChallengeSuccess(
    val orderId: String,
    val payerId: String
) : PayPalWebAuthChallengeResult
