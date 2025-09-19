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

    fun finish(intent: Intent, requestCode: Int, pendingStateBase64: String): BrowserSwitchFinishResult {
        val pendingState = BrowserSwitchPendingState.fromBase64(pendingStateBase64)
        return if (requestCode == pendingState.originalOptions.requestCode) {
            // TODO: parse success
//            return BrowserSwitchFinishResult.Success
            BrowserSwitchFinishResult.NoResult
        } else {
            BrowserSwitchFinishResult.NoResult
        }
    }
}
