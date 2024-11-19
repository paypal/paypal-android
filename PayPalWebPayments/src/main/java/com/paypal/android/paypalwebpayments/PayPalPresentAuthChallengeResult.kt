package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalPresentAuthChallengeResult {
    data class Success(val authState: String) : PayPalPresentAuthChallengeResult()
    data class Failure(val error: PayPalSDKError) : PayPalPresentAuthChallengeResult()
}
