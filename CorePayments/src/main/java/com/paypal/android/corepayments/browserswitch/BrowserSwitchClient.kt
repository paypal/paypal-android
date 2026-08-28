package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.common.DeviceInspector

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BrowserSwitchClient internal constructor(
    private val chromeCustomTabsClient: ChromeCustomTabsClient,
    private val authTabClient: AuthTabClient,
    private val deviceInspector: DeviceInspector
) {

    constructor(context: Context) : this(
        ChromeCustomTabsClient(),
        AuthTabClient(),
        DeviceInspector(context),
    )

    fun start(
        context: Context,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val activity = context as? Activity
        getValidationFailure(activity, options)?.let { return it }

        return when (options.launchMode) {
            BrowserSwitchLaunchMode.CUSTOM_TAB -> launchCustomTab(context, options)
            BrowserSwitchLaunchMode.AUTH_TAB -> launchAuthTab(requireNotNull(activity), options)
        }
    }

    private fun getValidationFailure(
        activity: Activity?,
        options: BrowserSwitchOptions,
    ): BrowserSwitchStartResult.Failure? {
        val returnUrlScheme = options.returnUrlScheme
        val appLinkUrl = options.appLinkUrl
        return if (activity != null && activity.isFinishing) {
            Failure.ActivityIsFinishing
        } else if (returnUrlScheme == null && appLinkUrl == null) {
            Failure.ReturnUrlSchemeAndAppLinkUrlBothNull
        } else if (returnUrlScheme != null && !hasValidDeepLinkConfig(returnUrlScheme)) {
            Failure.ManifestDeepLinkConfigurationInvalid
        } else if (options.launchMode == BrowserSwitchLaunchMode.AUTH_TAB && activity == null) {
            Failure.AuthTabActivityRequired
        } else {
            null
        }
    }

    private fun launchCustomTab(
        context: Context,
        options: BrowserSwitchOptions,
    ): BrowserSwitchStartResult {
        val customTabOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
        return when (chromeCustomTabsClient.launch(context, customTabOptions)) {
            LaunchChromeCustomTabResult.Success -> BrowserSwitchStartResult.Success
            LaunchChromeCustomTabResult.ActivityNotFound -> Failure.NoWebBrowser
        }
    }

    private fun launchAuthTab(
        activity: Activity,
        options: BrowserSwitchOptions,
    ): BrowserSwitchStartResult {
        return when (authTabClient.launch(activity, options)) {
            LaunchAuthTabResult.Success -> BrowserSwitchStartResult.Success
            LaunchAuthTabResult.ActivityNotFound -> Failure.NoWebBrowser
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
            val AuthTabActivityRequired = BrowserSwitchStartResult.Failure(
                Exception("Unable to launch Auth Tab without a source Activity.")
            )
        }
    }
}
