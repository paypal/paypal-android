package com.paypal.android.corepayments.chromecustomtabs

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.browserswitch.BrowserSwitchSession

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class TrackedChromeCustomTabResult(
    val launchResult: LaunchChromeCustomTabResult,
    val session: BrowserSwitchSession?
)
