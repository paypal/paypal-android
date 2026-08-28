package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthTabClient {

    fun launch(activity: Activity, options: BrowserSwitchOptions): LaunchAuthTabResult =
        try {
            activity.startActivity(AuthTabActivity.createIntent(activity, options))
            LaunchAuthTabResult.Success
        } catch (_: ActivityNotFoundException) {
            LaunchAuthTabResult.ActivityNotFound
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LaunchAuthTabResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Success : LaunchAuthTabResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object ActivityNotFound : LaunchAuthTabResult()
}
