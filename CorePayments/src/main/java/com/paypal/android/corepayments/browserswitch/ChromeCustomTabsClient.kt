package com.paypal.android.corepayments.browserswitch

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsIntent
import kotlin.jvm.Throws

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ChromeCustomTabOptions(
    val launchUri: Uri
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ChromeCustomTabsClient {

    @Throws(ActivityNotFoundException::class)
    fun launch(context: Context, options: ChromeCustomTabOptions) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, options.launchUri)
    }
}
