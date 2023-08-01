package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError

internal data class PayPalWebAuthChallengeError(
    val error: PayPalSDKError
) : PayPalWebAuthChallengeResult
