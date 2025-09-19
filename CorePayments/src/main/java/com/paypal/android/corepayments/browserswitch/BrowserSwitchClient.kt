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

    fun finish(
        intent: Intent,
        requestCode: Int,
        pendingStateBase64: String
    ): BrowserSwitchFinishResult {
        val pendingState = BrowserSwitchPendingState.fromBase64(pendingStateBase64)
        if (pendingState == null) {
            return BrowserSwitchFinishResult.PendingStateInvalid
        }

        val originalOptions = pendingState.originalOptions
        if (requestCode != originalOptions.requestCode) {
            return BrowserSwitchFinishResult.RequestCodeDoesNotMatch
        }

        val deepLinkUri = intent.data
        if (deepLinkUri == null) {
            return BrowserSwitchFinishResult.DeepLinkNotPresent
        }

        val deepLinkScheme = deepLinkUri.scheme.orEmpty()
        val isMatchingDeepLink =
            deepLinkScheme.equals(originalOptions.returnUrlScheme, ignoreCase = true)
        return if (isMatchingDeepLink) {
            BrowserSwitchFinishResult.Success(
                returnUrl = deepLinkUri,
                requestCode = originalOptions.requestCode,
                requestUrl = originalOptions.targetUri,
                requestMetadata = originalOptions.metadata
            )
        } else {
            BrowserSwitchFinishResult.DeepLinkDoesNotMatch
        }
    }
}
