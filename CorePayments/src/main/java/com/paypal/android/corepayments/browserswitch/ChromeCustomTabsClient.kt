package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

data class ChromeCustomTabOptions(
    val launchUri: Uri
)

class ChromeCustomTabsClient {
    fun launch(context: Context, options: ChromeCustomTabOptions) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, options.launchUri)
    }
}
