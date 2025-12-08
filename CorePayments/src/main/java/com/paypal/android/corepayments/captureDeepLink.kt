package com.paypal.android.corepayments

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import kotlin.text.equals

sealed class DeepLink(val uri: Uri, val originalOptions: BrowserSwitchOptions)

class PayPalVaultComplete(uri: Uri, originalOptions: BrowserSwitchOptions) :
    DeepLink(uri, originalOptions)

class PayPalCheckoutComplete(uri: Uri, originalOptions: BrowserSwitchOptions) :
    DeepLink(uri, originalOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CaptureDeepLinkResult<out T : DeepLink>() {
    data class Success<T : DeepLink>(val deepLink: T) : CaptureDeepLinkResult<T>()
    object RequestCodeDoesNotMatch : CaptureDeepLinkResult<Nothing>()
    object DeepLinkNotPresent : CaptureDeepLinkResult<Nothing>()
    object DeepLinkDoesNotMatch : CaptureDeepLinkResult<Nothing>()
    object UnknownError : CaptureDeepLinkResult<Nothing>()
    object AuthStateInvalid : CaptureDeepLinkResult<Nothing>()
}

// Ref: https://stackoverflow.com/a/33158859
inline fun <reified T : DeepLink> isPayPalCheckout(klass: Class<T>) =
    klass.isAssignableFrom(PayPalCheckoutComplete::class.java)

inline fun <reified T : DeepLink> isPayPalVault(klass: Class<T>) =
    klass.isAssignableFrom(PayPalVaultComplete::class.java)

fun isCustomSchemeMatch(options: BrowserSwitchOptions, uri: Uri) =
    uri.scheme.orEmpty().equals(options.returnUrlScheme, ignoreCase = true)

fun isAppLinkMatch(options: BrowserSwitchOptions, uri: Uri): Boolean {
    val appLinkUrl = options.appLinkUrl?.toUri()
    if (appLinkUrl != null) {
        val hasMatchingScheme = uri.scheme?.equals(appLinkUrl.scheme) ?: false
        val hasMatchingHost = uri.host?.equals(appLinkUrl.host) ?: false
        return hasMatchingScheme && hasMatchingHost
    }
    return false
}

inline fun <reified T : DeepLink> captureDeepLink(
    intent: Intent,
    authState: String
): CaptureDeepLinkResult<T> {
    val pendingState = BrowserSwitchPendingState.fromBase64(authState)
    if (pendingState == null) {
        return CaptureDeepLinkResult.AuthStateInvalid
    }

    val options = pendingState.originalOptions
    val klass = T::class.java
    val requestCode = if (isPayPalVault(klass)) {
        BrowserSwitchRequestCodes.PAYPAL_VAULT
    } else if (isPayPalCheckout(klass)) {
        BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
    } else {
        null
    }

    if (requestCode != options.requestCode) {
        return CaptureDeepLinkResult.RequestCodeDoesNotMatch
    }

    val deepLinkUri = intent.data
    if (deepLinkUri == null) {
        return CaptureDeepLinkResult.DeepLinkNotPresent
    }

    val isMatchingDeepLink =
        isCustomSchemeMatch(options, deepLinkUri) || isAppLinkMatch(options, deepLinkUri)
    return if (isMatchingDeepLink) {
        val deepLink = if (isPayPalVault(klass)) {
            PayPalVaultComplete(deepLinkUri, options)
        } else if (isPayPalCheckout(klass)) {
            PayPalCheckoutComplete(deepLinkUri, options)
        } else {
            null
        }
        if (deepLink is T) {
            CaptureDeepLinkResult.Success(deepLink)
        } else {
            CaptureDeepLinkResult.UnknownError
        }
    } else {
        CaptureDeepLinkResult.DeepLinkDoesNotMatch
    }
}