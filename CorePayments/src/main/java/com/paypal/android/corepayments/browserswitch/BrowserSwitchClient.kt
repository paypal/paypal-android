package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent

class BrowserSwitchClient {
    fun start(
        context: Context,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, options.targetUri)
        val pendingState = BrowserSwitchPendingState(options)
        return BrowserSwitchStartResult.Success(pendingState)
    }

    fun finish(intent: Intent, pendingStateBase64: String): BrowserSwitchFinishResult {
        // TODO: implement
        return BrowserSwitchFinishResult.NoResult
    }
}
