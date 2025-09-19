package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState

sealed class PayPalWebLaunchResult {
    data class Success(val pendingState: BrowserSwitchPendingState) : PayPalWebLaunchResult()
    data class Failure(val error: PayPalSDKError) : PayPalWebLaunchResult()
}
