package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardAuthChallengeResult {
    data class Success(val authState: String) : CardAuthChallengeResult()
    data class Failure(val error: PayPalSDKError) : CardAuthChallengeResult()
}