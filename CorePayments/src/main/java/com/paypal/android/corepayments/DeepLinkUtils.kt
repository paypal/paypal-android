package com.paypal.android.corepayments

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DeepLink(val uri: Uri, val originalOptions: BrowserSwitchOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CaptureDeepLinkResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Success(val deepLink: DeepLink) : CaptureDeepLinkResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object RequestCodeDoesNotMatch : CaptureDeepLinkResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object DeepLinkNotPresent : CaptureDeepLinkResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object DeepLinkDoesNotMatch : CaptureDeepLinkResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object AuthStateInvalid : CaptureDeepLinkResult()
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
        return CaptureDeepLinkResult.AuthStateInvalid
    }

    val options = pendingState.originalOptions
    if (requestCode != options.requestCode) {
        return CaptureDeepLinkResult.RequestCodeDoesNotMatch
    }

    val deepLinkUri = intent.data
    if (deepLinkUri == null) {
        return CaptureDeepLinkResult.DeepLinkNotPresent
    }

    val isMatchingDeepLink =
        isCustomSchemeMatch(deepLinkUri, options) || isAppLinkMatch(deepLinkUri, options)
    return if (isMatchingDeepLink) {
        val deepLink = DeepLink(deepLinkUri, pendingState.originalOptions)
        CaptureDeepLinkResult.Success(deepLink)
    } else {
        CaptureDeepLinkResult.DeepLinkDoesNotMatch
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
