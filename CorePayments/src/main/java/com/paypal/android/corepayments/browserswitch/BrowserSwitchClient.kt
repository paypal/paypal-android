package com.paypal.android.corepayments.browserswitch

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity

class BrowserSwitchClient internal constructor(
    private val customsInternalClient: ChromeCustomTabsInternalClient
) {

    constructor() : this(ChromeCustomTabsInternalClient())

    fun launch(
        activity: ComponentActivity,
        request: BrowserSwitchOptions
    ): BrowserSwitchLaunchResult {
        customsInternalClient.launchUrl(activity, request.urlToOpen, false)
        return BrowserSwitchLaunchResult.Success
    }

    fun parseStatus(intent: Intent, options: BrowserSwitchOptions): BrowserSwitchStatus? =
        intent.data?.let { returnUrl ->
            return if (checkForMatchingDeepLinkUrl(returnUrl, options)) {
                BrowserSwitchStatus.Complete(returnUrl, options)
            } else {
                BrowserSwitchStatus.NoResult
            }
        }

    private fun checkForMatchingDeepLinkUrl(url: Uri, options: BrowserSwitchOptions): Boolean {
        val actualScheme = url.scheme
        val targetScheme = Uri.parse(options.returnUrl).scheme
        if (actualScheme == null && targetScheme == null) {
            return false
        }
        return actualScheme.equals(targetScheme, ignoreCase = true)
    }
}