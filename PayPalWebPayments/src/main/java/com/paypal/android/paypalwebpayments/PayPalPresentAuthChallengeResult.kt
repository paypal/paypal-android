package com.paypal.android.paypalwebpayments

import android.net.Uri
import com.paypal.android.corepayments.PayPalSDKError

sealed class PayPalPresentAuthChallengeResult {
    data class Success(val authState: String, val uri: Uri) : PayPalPresentAuthChallengeResult()
    data class Failure(val error: PayPalSDKError) : PayPalPresentAuthChallengeResult()
}
