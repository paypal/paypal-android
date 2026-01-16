package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.common.DeviceInspector

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BrowserSwitchClient(
    private val chromeCustomTabsClient: ChromeCustomTabsClient,
    private val authTabClient: AuthTabClient,
    private val deviceInspector: DeviceInspector
) {

    constructor(context: Context) : this(
        ChromeCustomTabsClient(),
        AuthTabClient(),
        DeviceInspector(context)
    )

    fun start(
        context: Context,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val activity = context as? Activity
        val returnUrlScheme = options.returnUrlScheme
        val appLinkUrl = options.appLinkUrl
        return if (activity != null && activity.isFinishing) {
            Failure.ActivityIsFinishing
        } else if (returnUrlScheme == null && appLinkUrl == null) {
            Failure.ReturnUrlSchemeAndAppLinkUrlBothNull
        } else if (returnUrlScheme != null && !hasValidDeepLinkConfig(returnUrlScheme)) {
            Failure.ManifestDeepLinkConfigurationInvalid
        } else {
            val cctOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
            when (chromeCustomTabsClient.launch(context, cctOptions)) {
                LaunchChromeCustomTabResult.Success -> BrowserSwitchStartResult.Success
                LaunchChromeCustomTabResult.ActivityNotFound -> Failure.NoWebBrowser
            }
        }
    }

    // check for invalid deep link configuration in AndroidManifest.xml
    private fun hasValidDeepLinkConfig(returnUrlScheme: String) =
        deviceInspector.isDeepLinkConfiguredInManifest(returnUrlScheme)

    internal companion object {
        object Failure {
            val ActivityIsFinishing = BrowserSwitchStartResult.Failure(
                Exception(
                    "Unable to launch Chrome Custom Tab while the source Activity is finishing."
                )
            )
            val ReturnUrlSchemeAndAppLinkUrlBothNull = BrowserSwitchStartResult.Failure(
                Exception(
                    "The properties 'returnUrlScheme' and 'appLinkUrl' cannot both be null."
                )
            )
            val NoWebBrowser = BrowserSwitchStartResult.Failure(
                Exception(
                    "Unable to launch Chrome Custom Tab on device without a web browser."
                )
            )
            val ManifestDeepLinkConfigurationInvalid = BrowserSwitchStartResult.Failure(
                Exception(
                    "This app is not correctly configured to handle deep links from the return url scheme provided."
                )
            )
        }
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
