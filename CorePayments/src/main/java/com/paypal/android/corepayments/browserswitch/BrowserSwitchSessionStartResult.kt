package com.paypal.android.corepayments.browserswitch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BrowserSwitchSessionStartResult(
    val startResult: BrowserSwitchStartResult,
    val session: BrowserSwitchSession?
)
