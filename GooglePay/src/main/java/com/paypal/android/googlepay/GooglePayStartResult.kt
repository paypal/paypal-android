package com.paypal.android.googlepay

import com.paypal.android.corepayments.PayPalSDKError

sealed class GooglePayStartResult {
    class Success(val authChallenge: GooglePayAuthChallenge) : GooglePayStartResult()
    class Failure(val error: PayPalSDKError) : GooglePayStartResult()
}
