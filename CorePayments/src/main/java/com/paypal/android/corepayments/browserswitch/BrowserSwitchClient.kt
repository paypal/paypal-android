package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BrowserSwitchClient(
    private val chromeCustomTabsClient: ChromeCustomTabsClient = ChromeCustomTabsClient()
) {
    fun start(
        context: Context,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val cctOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
        chromeCustomTabsClient.launch(context, cctOptions)
        return BrowserSwitchStartResult.Success
    }
}
