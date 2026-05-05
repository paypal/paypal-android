package com.paypal.android.googlepay

import com.paypal.android.corepayments.PayPalSDKError

sealed class GooglePayFinishStartResult {
    class Success(
        val status: String,
        val cardLastDigits: String,
        val cardType: String,
        val cardBrand: String
    ) : GooglePayFinishStartResult()

    class Failure(val error: PayPalSDKError) : GooglePayFinishStartResult()
}
