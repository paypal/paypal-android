package com.paypal.android.corepayments.browserswitch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LaunchChromeCustomTabResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Success : LaunchChromeCustomTabResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object ActivityNotFound : LaunchChromeCustomTabResult()
}
