package com.paypal.android.corepayments.browserswitch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class BrowserSwitchStartResult {
    object Success : BrowserSwitchStartResult()
    class Failure(val error: Exception) : BrowserSwitchStartResult()
}
