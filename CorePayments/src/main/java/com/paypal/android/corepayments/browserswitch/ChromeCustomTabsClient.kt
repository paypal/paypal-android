package com.paypal.android.corepayments.browserswitch

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsIntent

internal class ChromeCustomTabsClient {

    fun launch(context: Context, options: ChromeCustomTabOptions): LaunchChromeCustomTabResult {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        return try {
            customTabsIntent.launchUrl(context, options.launchUri)
            LaunchChromeCustomTabResult.Success
        } catch (_: ActivityNotFoundException) {
            LaunchChromeCustomTabResult.ActivityNotFound
        }
    }
}

internal data class ChromeCustomTabOptions(val launchUri: Uri)

internal sealed class LaunchChromeCustomTabResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Success : LaunchChromeCustomTabResult()
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object ActivityNotFound : LaunchChromeCustomTabResult()
}
