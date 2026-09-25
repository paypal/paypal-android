package com.paypal.android.corepayments

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import com.paypal.android.corepayments.browserswitch.AuthTabClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DeepLink(val uri: Uri, val originalOptions: BrowserSwitchOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CaptureDeepLinkResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Success(val deepLink: DeepLink) : CaptureDeepLinkResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failure(val reason: PayPalSDKError) : CaptureDeepLinkResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Canceled(val originalOptions: BrowserSwitchOptions) : CaptureDeepLinkResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Ignore(val debugMessage: String) : CaptureDeepLinkResult()
}

// TODO: see if we can resolve ReturnCount lint error instead of suppressing it
@Suppress("ReturnCount")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun captureDeepLink(
    requestCode: Int,
    intent: Intent,
    authState: String
): CaptureDeepLinkResult {
    val pendingState = BrowserSwitchPendingState.fromBase64(authState)
    if (pendingState == null) {
        // TODO: remove error codes and error description from project; the built in
        // Throwable type already has a message property and error codes are only required
        // for iOS Error protocol conformance
        val reason = PayPalSDKError(0, "Auth state invalid.")
        return CaptureDeepLinkResult.Failure(reason)
    }

    val options = pendingState.originalOptions
    if (requestCode != options.requestCode) {
        return CaptureDeepLinkResult.Ignore("Request code does not match.")
    }

    if (intent.hasExtra(AuthTabClient.EXTRA_AUTH_TAB_RESULT_CODE)) {
        return captureAuthTabResult(intent, options)
    }

    return captureDeepLinkUri(intent, options)
}

private fun captureAuthTabResult(
    intent: Intent,
    options: BrowserSwitchOptions
): CaptureDeepLinkResult {
    val resultCode = intent.getIntExtra(
        AuthTabClient.EXTRA_AUTH_TAB_RESULT_CODE,
        AuthTabIntent.RESULT_UNKNOWN_CODE
    )

    return when (resultCode) {
        AuthTabIntent.RESULT_CANCELED -> CaptureDeepLinkResult.Canceled(options)
        AuthTabIntent.RESULT_OK -> captureDeepLinkUri(intent, options)
        else -> {
            val reason = PayPalSDKError(
                code = 0,
                errorDescription = "Auth Tab failed with result code $resultCode."
            )
            CaptureDeepLinkResult.Failure(reason)
        }
    }
}

private fun captureDeepLinkUri(
    intent: Intent,
    options: BrowserSwitchOptions,
): CaptureDeepLinkResult {
    val deepLinkUri = intent.data
    if (deepLinkUri == null) {
        return CaptureDeepLinkResult.Ignore("Intent data is null.")
    }

    val isMatchingDeepLink =
        isCustomSchemeMatch(deepLinkUri, options) || isAppLinkMatch(deepLinkUri, options)
    return if (isMatchingDeepLink) {
        val deepLink = DeepLink(deepLinkUri, options)
        CaptureDeepLinkResult.Success(deepLink)
    } else {
        val message = "Deep link custom scheme or host is not associated with the original request."
        CaptureDeepLinkResult.Ignore(message)
    }
}

private fun isCustomSchemeMatch(uri: Uri, options: BrowserSwitchOptions) =
    uri.scheme.orEmpty().equals(options.returnUrlScheme, ignoreCase = true)

private fun isAppLinkMatch(uri: Uri, options: BrowserSwitchOptions): Boolean {
    val appLinkUrl = options.appLinkUrl?.toUri()
    if (appLinkUrl != null) {
        val hasMatchingScheme = uri.scheme?.equals(appLinkUrl.scheme) ?: false
        val hasMatchingHost = uri.host?.equals(appLinkUrl.host) ?: false
        return hasMatchingScheme && hasMatchingHost
    }
    return false
}
