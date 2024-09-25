package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalAuthChallengeResult {
    data class Success(val authState: String) : PayPalAuthChallengeResult()
    data class Failure(val error: PayPalSDKError) : PayPalAuthChallengeResult()
}