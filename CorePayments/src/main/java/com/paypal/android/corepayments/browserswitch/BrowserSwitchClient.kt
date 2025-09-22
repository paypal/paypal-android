package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent

class BrowserSwitchClient(
    private val chromeCustomTabsClient: ChromeCustomTabsClient = ChromeCustomTabsClient()
) {
    fun start(
        context: Context,
        options: BrowserSwitchOptions
    ): BrowserSwitchStartResult {
        val cctOptions = ChromeCustomTabOptions(launchUri = options.targetUri)
        chromeCustomTabsClient.launch(context, cctOptions)
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
