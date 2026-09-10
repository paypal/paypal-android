package com.paypal.android.corepayments.browserswitch

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri

internal data class PendingRequest(
    val encodedBrowserSwitchState: String,
    val fallbackResultUri: Uri?,
) {
    fun toBundle() = Bundle().apply {
        putString(KEY_BROWSER_SWITCH_STATE, encodedBrowserSwitchState)
        putString(KEY_FALLBACK_RESULT_URI, fallbackResultUri?.toString())
    }

    companion object {
        fun from(options: BrowserSwitchOptions): PendingRequest {
            val fallbackResultUri = options.appLinkUrl?.toUri()
                ?: options.returnUrlScheme?.let { scheme ->
                    "$scheme://x-callback-url/paypal-sdk/paypal-checkout/cancel".toUri()
                }
            // The launch URL is not needed to finish a returned checkout and may contain
            // short-lived identifiers. Persist only the fields captureDeepLink() consumes.
            val completionOptions = options.copy(targetUri = Uri.EMPTY)
            return PendingRequest(
                encodedBrowserSwitchState = BrowserSwitchPendingState(completionOptions)
                    .toBase64EncodedJSON(),
                fallbackResultUri = fallbackResultUri,
            )
        }

        fun from(bundle: Bundle): PendingRequest? {
            val encodedState = bundle.getString(KEY_BROWSER_SWITCH_STATE)
            val pendingState = encodedState?.let(BrowserSwitchPendingState::fromBase64)
            return if (
                encodedState != null &&
                pendingState?.originalOptions?.launchMode == BrowserSwitchLaunchMode.AUTH_TAB
            ) {
                PendingRequest(
                    encodedBrowserSwitchState = encodedState,
                    fallbackResultUri = bundle.getString(KEY_FALLBACK_RESULT_URI)?.toUri(),
                )
            } else {
                null
            }
        }

        private const val KEY_BROWSER_SWITCH_STATE = "browserSwitchState"
        private const val KEY_FALLBACK_RESULT_URI = "fallbackResultUri"
    }
}
