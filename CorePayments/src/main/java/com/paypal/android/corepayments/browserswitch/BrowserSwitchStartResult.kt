package com.paypal.android.corepayments.browserswitch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class BrowserSwitchStartResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Success : BrowserSwitchStartResult()
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failure(val error: Exception) : BrowserSwitchStartResult()
}
