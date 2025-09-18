package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardPresentAuthChallengeResult {
    data class Success(internal val authStateInternal: String) : CardPresentAuthChallengeResult() {
        // TODO: v3 – remove public facing authState property
        @Deprecated("This accessor is no longer supported. Auth state is now managed internally by the SDK.")
        val authState: String = authStateInternal
    }
    data class Failure(val error: PayPalSDKError) : CardPresentAuthChallengeResult()
}
