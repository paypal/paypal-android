package com.paypal.android.venmo

import com.paypal.android.corepayments.PayPalSDKError

sealed class VenmoFinishStartResult {
    data class Success(
        val token: String,
        val payerId: String,
        val approved: Boolean
    ) : VenmoFinishStartResult()

    data class Canceled(
        val orderId: String? = null
    ) : VenmoFinishStartResult()

    data object NoResult : VenmoFinishStartResult()

    data class Failure(val error: PayPalSDKError) : VenmoFinishStartResult()
}

