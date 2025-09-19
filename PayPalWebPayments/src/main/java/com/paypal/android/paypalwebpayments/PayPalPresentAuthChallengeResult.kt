package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalPresentAuthChallengeResult {
    data class Success(
        // TODO: v3 – remove public facing authState property
        @Deprecated("This accessor is no longer supported. Auth state is now managed internally by the SDK.")
        internal val authState: String
    ) : PayPalPresentAuthChallengeResult()

    data class Failure(val error: PayPalSDKError) : PayPalPresentAuthChallengeResult()
}
