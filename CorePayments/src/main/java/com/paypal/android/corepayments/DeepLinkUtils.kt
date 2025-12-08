package com.paypal.android.corepayments

import android.content.Intent
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class DeepLink(val uri: Uri, val originalOptions: BrowserSwitchOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardApproveOrderComplete(uri: Uri, originalOptions: BrowserSwitchOptions) :
    DeepLink(uri, originalOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardVaultComplete(uri: Uri, originalOptions: BrowserSwitchOptions) :
    DeepLink(uri, originalOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayPalVaultComplete(uri: Uri, originalOptions: BrowserSwitchOptions) :
    DeepLink(uri, originalOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayPalCheckoutComplete(uri: Uri, originalOptions: BrowserSwitchOptions) :
    DeepLink(uri, originalOptions)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CaptureDeepLinkResult<out T : DeepLink> {
    data class Success<T : DeepLink>(val deepLink: T) : CaptureDeepLinkResult<T>()
    object RequestCodeDoesNotMatch : CaptureDeepLinkResult<Nothing>()
    object DeepLinkNotPresent : CaptureDeepLinkResult<Nothing>()
    object DeepLinkDoesNotMatch : CaptureDeepLinkResult<Nothing>()
    object UnknownError : CaptureDeepLinkResult<Nothing>()
    object AuthStateInvalid : CaptureDeepLinkResult<Nothing>()
}

// Ref: https://stackoverflow.com/a/33158859
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : DeepLink> isCardApproveOrder(klass: Class<T>) =
    klass.isAssignableFrom(CardApproveOrderComplete::class.java)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : DeepLink> isCardVault(klass: Class<T>) =
    klass.isAssignableFrom(CardVaultComplete::class.java)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : DeepLink> isPayPalCheckout(klass: Class<T>) =
    klass.isAssignableFrom(PayPalCheckoutComplete::class.java)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : DeepLink> isPayPalVault(klass: Class<T>) =
    klass.isAssignableFrom(PayPalVaultComplete::class.java)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun isCustomSchemeMatch(uri: Uri, options: BrowserSwitchOptions) =
    uri.scheme.orEmpty().equals(options.returnUrlScheme, ignoreCase = true)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun isAppLinkMatch(uri: Uri, options: BrowserSwitchOptions): Boolean {
    val appLinkUrl = options.appLinkUrl?.toUri()
    if (appLinkUrl != null) {
        val hasMatchingScheme = uri.scheme?.equals(appLinkUrl.scheme) ?: false
        val hasMatchingHost = uri.host?.equals(appLinkUrl.host) ?: false
        return hasMatchingScheme && hasMatchingHost
    }
    return false
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : DeepLink> getRequestCode(): Int? {
    val klass = T::class.java
    return if (isCardApproveOrder(klass)) {
        BrowserSwitchRequestCodes.CARD_APPROVE_ORDER
    } else if (isCardVault(klass)) {
        BrowserSwitchRequestCodes.CARD_VAULT
    } else if (isPayPalVault(klass)) {
        BrowserSwitchRequestCodes.PAYPAL_VAULT
    } else if (isPayPalCheckout(klass)) {
        BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
    } else {
        null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : DeepLink> buildDeepLink(
    uri: Uri,
    originalOptions: BrowserSwitchOptions
): DeepLink? {
    val klass = T::class.java
    return if (isCardApproveOrder(klass)) {
        CardApproveOrderComplete(uri, originalOptions)
    } else if (isCardVault(klass)) {
        CardVaultComplete(uri, originalOptions)
    } else if (isPayPalVault(klass)) {
        PayPalVaultComplete(uri, originalOptions)
    } else if (isPayPalCheckout(klass)) {
        PayPalCheckoutComplete(uri, originalOptions)
    } else {
        null
    }
}

// TODO: see if we can resolve ReturnCount lint error instead of suppressing it
@Suppress("ReturnCount")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : DeepLink> captureDeepLink(
    intent: Intent,
    authState: String
): CaptureDeepLinkResult<T> {
    val pendingState = BrowserSwitchPendingState.fromBase64(authState)
    if (pendingState == null) {
        return CaptureDeepLinkResult.AuthStateInvalid
    }

    val options = pendingState.originalOptions
    val requestCode = getRequestCode<T>()
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
        val deepLink = buildDeepLink<T>(deepLinkUri, options)
        if (deepLink is T) {
            CaptureDeepLinkResult.Success(deepLink)
        } else {
            CaptureDeepLinkResult.UnknownError
        }
    } else {
        CaptureDeepLinkResult.DeepLinkDoesNotMatch
    }
}
