package com.paypal.android.corepayments

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DeepLinkV2(val uri: Uri, val originalOptions: BrowserSwitchOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CaptureDeepLinkResultV2 {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Success(val deepLink: DeepLinkV2) : CaptureDeepLinkResultV2()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object RequestCodeDoesNotMatch : CaptureDeepLinkResultV2()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object DeepLinkNotPresent : CaptureDeepLinkResultV2()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object DeepLinkDoesNotMatch : CaptureDeepLinkResultV2()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object AuthStateInvalid : CaptureDeepLinkResultV2()
}

// TODO: see if we can resolve ReturnCount lint error instead of suppressing it
@Suppress("ReturnCount")
fun captureDeepLinkV2(
    requestCode: Int,
    intent: Intent,
    authState: String
): CaptureDeepLinkResultV2 {
    val pendingState = BrowserSwitchPendingState.fromBase64(authState)
    if (pendingState == null) {
        return CaptureDeepLinkResultV2.AuthStateInvalid
    }

    val options = pendingState.originalOptions
    if (requestCode != options.requestCode) {
        return CaptureDeepLinkResultV2.RequestCodeDoesNotMatch
    }

    val deepLinkUri = intent.data
    if (deepLinkUri == null) {
        return CaptureDeepLinkResultV2.DeepLinkNotPresent
    }

    val isMatchingDeepLink =
        isCustomSchemeMatch(deepLinkUri, options) || isAppLinkMatch(deepLinkUri, options)
    return if (isMatchingDeepLink) {
        val deepLink = DeepLinkV2(deepLinkUri, pendingState.originalOptions)
        CaptureDeepLinkResultV2.Success(deepLink)
    } else {
        CaptureDeepLinkResultV2.DeepLinkDoesNotMatch
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
