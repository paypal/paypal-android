package com.paypal.android.corepayments.browserswitch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class TrackedChromeCustomTabResult(
    val launchResult: LaunchChromeCustomTabResult,
    val session: BrowserSwitchSession?
)
