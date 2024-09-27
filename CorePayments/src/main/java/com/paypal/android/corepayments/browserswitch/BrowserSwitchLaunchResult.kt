package com.paypal.android.corepayments.browserswitch

sealed class BrowserSwitchLaunchResult {
    object Success: BrowserSwitchLaunchResult()
    class Failure(val error: Throwable): BrowserSwitchLaunchResult()
}
