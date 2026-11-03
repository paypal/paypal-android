package com.paypal.android.venmo

import com.paypal.android.corepayments.PayPalSDKError

sealed class VenmoStartResult {
    data object Success : VenmoStartResult()

    data class Failure(val error: PayPalSDKError) : VenmoStartResult()
}
