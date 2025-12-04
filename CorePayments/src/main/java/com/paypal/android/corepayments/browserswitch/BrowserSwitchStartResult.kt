package com.paypal.android.corepayments.browserswitch

sealed class BrowserSwitchStartResult() {
    object Success: BrowserSwitchStartResult()
    class Failure(val error: Exception): BrowserSwitchStartResult()
}
