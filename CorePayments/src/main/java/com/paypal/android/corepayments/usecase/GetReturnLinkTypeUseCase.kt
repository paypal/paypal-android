package com.paypal.android.corepayments.usecase

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.common.DeviceInspector

/**
 * Decides which return-link strategy to use when navigating from App Switch or the browser back
 * into the merchant app after a checkout/vault flow.
 *
 * Returns [ReturnLinkTypeResult.APP_LINK] when the merchant app is the verified default handler for
 * its own App Link return URL AND either the first-party PayPal app or an App-Links-compatible
 * browser will honor the return. Otherwise returns [ReturnLinkTypeResult.DEEP_LINK], signalling that
 * the custom URL scheme fallback should be used instead.
 *
 * Mirrors braintree_android's `GetReturnLinkTypeUseCase`, reusing [DeviceInspector] for the
 * PayPal-app app-switch check.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnLinkTypeUseCase(
    private val applicationContext: Context,
    private val deviceInspector: DeviceInspector,
    private val getDefaultAppUseCase: GetDefaultAppUseCase,
    private val getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase,
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ReturnLinkTypeResult {
        APP_LINK, DEEP_LINK
    }

    /**
     * @param appLinkReturnUri The merchant's App Link return URL (`returnAppUrl`).
     * @param checkoutUri The checkout URL used to probe whether the default browser is
     *   App-Links-compatible. Defaults to PayPal's checkout URL.
     */
    operator fun invoke(
        appLinkReturnUri: Uri?,
        checkoutUri: Uri = DEFAULT_CHECKOUT_URI.toUri(),
    ): ReturnLinkTypeResult {
        val appLinkWillRoute = isMerchantDefaultHandler(appLinkReturnUri) &&
            (deviceInspector.canResolvePayPalAppSwitch() || getAppLinksCompatibleBrowserUseCase(checkoutUri))
        return if (appLinkWillRoute) ReturnLinkTypeResult.APP_LINK else ReturnLinkTypeResult.DEEP_LINK
    }

    /**
     * True when the merchant app is the default handler for its own App Link return URI. Returns
     * false when the shopper has unchecked "Open supported links" for the merchant app in Android
     * settings, or when App Link (Digital Asset Links) verification isn't configured.
     */
    private fun isMerchantDefaultHandler(appLinkReturnUri: Uri?): Boolean =
        appLinkReturnUri != null &&
            applicationContext.packageName == getDefaultAppUseCase(appLinkReturnUri)

    private companion object {
        private const val DEFAULT_CHECKOUT_URI = "https://www.paypal.com/checkout"
    }
}
