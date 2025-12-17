package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BrowserSwitchClient(
    private val chromeCustomTabsClient: ChromeCustomTabsClient = ChromeCustomTabsClient(),
    private val authTabClient: AuthTabClient = AuthTabClient()
) {
    fun start(
        context: Context,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val cctOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
        chromeCustomTabsClient.launch(context, cctOptions)
        return BrowserSwitchStartResult.Success
    }

    fun start(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val cctOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
        authTabClient.launchAuthTab(
            options = cctOptions,
            activityResultLauncher = activityResultLauncher,
            appLinkUrl = options.appLinkUrl,
            returnUrlScheme = options.returnUrlScheme
        )
        return BrowserSwitchStartResult.Success
    }
}
