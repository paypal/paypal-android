package com.paypal.android.venmo

import com.paypal.android.corepayments.PayPalSDKError

sealed class VenmoPresentAuthChallengeResult {
    data object Success : VenmoPresentAuthChallengeResult()

    data class Failure(val error: PayPalSDKError) : VenmoPresentAuthChallengeResult()
}
