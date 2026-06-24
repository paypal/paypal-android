package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardPresentAuthChallengeResult {
    class Success internal constructor(
        internal val authState: String
    ) : CardPresentAuthChallengeResult()

    data class Failure(val error: PayPalSDKError) : CardPresentAuthChallengeResult()
}
