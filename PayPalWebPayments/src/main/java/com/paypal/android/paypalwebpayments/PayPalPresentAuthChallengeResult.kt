package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalPresentAuthChallengeResult {
    class Success internal constructor(
        internal val authState: String
    ) : PayPalPresentAuthChallengeResult()

    data class Failure(val error: PayPalSDKError) : PayPalPresentAuthChallengeResult()
}
