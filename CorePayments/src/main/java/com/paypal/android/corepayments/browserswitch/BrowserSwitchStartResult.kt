package com.paypal.android.corepayments.browserswitch

sealed class BrowserSwitchStartResult() {
    class Success(val pendingState: BrowserSwitchPendingState): BrowserSwitchStartResult()
    class Failure(val error: Exception): BrowserSwitchStartResult()
}
