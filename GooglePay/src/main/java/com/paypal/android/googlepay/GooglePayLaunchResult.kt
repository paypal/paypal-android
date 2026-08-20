package com.paypal.android.googlepay

import com.paypal.android.corepayments.PayPalSDKError

sealed class GooglePayLaunchResult {
    data class Success(internal val paymentMethodData: String) : GooglePayLaunchResult()
    data class Failure(val error: PayPalSDKError) : GooglePayLaunchResult()
    data object UserCanceled : GooglePayLaunchResult()
}
