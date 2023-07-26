package com.paypal.android.cardpayments

internal data class CardAuthChallengeError(
    val message: String
) : CardAuthChallengeResult
