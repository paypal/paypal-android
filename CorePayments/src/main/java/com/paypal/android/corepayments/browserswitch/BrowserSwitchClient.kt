package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BrowserSwitchClient(
    private val chromeCustomTabsClient: ChromeCustomTabsClient = ChromeCustomTabsClient()
) {
    fun start(
        context: Context,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val activity = context as? Activity
        if (activity != null && activity.isFinishing) {
            val message =
                "Unable to launch Chrome Custom Tab while the source Activity is finishing."
            return BrowserSwitchStartResult.Failure(Exception(message))
        }

        val cctOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
        return when (chromeCustomTabsClient.launch(context, cctOptions)) {
            LaunchChromeCustomTabResult.Success -> BrowserSwitchStartResult.Success
            LaunchChromeCustomTabResult.ActivityNotFound -> {
                val message =
                    "Unable to launch Chrome Custom Tab on device without a web browser."
                return BrowserSwitchStartResult.Failure(Exception(message))
            }
        }
    }
}
