package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardPresentAuthChallengeResult {
    data class Success(val authState: String) : CardPresentAuthChallengeResult()
    data class Failure(val error: PayPalSDKError) : CardPresentAuthChallengeResult()
}
