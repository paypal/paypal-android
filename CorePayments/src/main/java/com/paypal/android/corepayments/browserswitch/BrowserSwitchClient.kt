package com.paypal.android.corepayments.browserswitch

import android.content.Intent
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

    fun parseStatus(intent: Intent, options: BrowserSwitchOptions): BrowserSwitchStatus? {
        return null
    }
}