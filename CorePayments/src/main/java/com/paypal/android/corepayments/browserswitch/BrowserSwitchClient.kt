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
        return if (activity != null && activity.isFinishing) {
            Failure.ActivityIsFinishing
        } else if (isManifestDeepLinkConfigInvalid(context, options)) {
            Failure.ManifestDeepLinkConfigurationInvalid
        } else {
            val cctOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
            when (chromeCustomTabsClient.launch(context, cctOptions)) {
                LaunchChromeCustomTabResult.Success -> BrowserSwitchStartResult.Success
                LaunchChromeCustomTabResult.ActivityNotFound -> Failure.NoWebBrowser
            }
        }
    }

    private fun isManifestDeepLinkConfigInvalid(
        context: Context,
        options: BrowserSwitchOptions
    ): Boolean {
        return false
    }

    companion object {
        object Failure {
            val ActivityIsFinishing = BrowserSwitchStartResult.Failure(
                Exception("Unable to launch Chrome Custom Tab while the source Activity is finishing.")
            )
            val NoWebBrowser = BrowserSwitchStartResult.Failure(
                Exception("Unable to launch Chrome Custom Tab on device without a web browser.")
            )
            val ManifestDeepLinkConfigurationInvalid = BrowserSwitchStartResult.Failure(
                Exception("TODO: implement")
            )
        }
    }
}
