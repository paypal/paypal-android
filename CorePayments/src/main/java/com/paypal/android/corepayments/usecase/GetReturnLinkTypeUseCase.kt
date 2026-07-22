package com.paypal.android.corepayments.usecase

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.LinkType
import com.paypal.android.corepayments.common.DeviceInspector

/**
 * Decides which return-link strategy to use when navigating from App Switch or the browser back
 * into the merchant app after a checkout/vault flow.
 *
 * Returns [LinkType.APP_LINK] when the merchant app is the verified default handler for
 * its own App Link return URL AND either the first-party PayPal app or an App-Links-compatible
 * browser will honor the return. Otherwise returns [LinkType.DEEP_LINK], signalling that
 * the custom URL scheme fallback should be used instead.
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

    /**
     * @param appLinkReturnUri The merchant's App Link return URL (`returnAppUrl`).
     * @param checkoutUri The checkout URL used to probe whether the default browser is
     *   App-Links-compatible. Defaults to PayPal's checkout URL.
     */
    operator fun invoke(
        appLinkReturnUri: Uri?,
        checkoutUri: Uri = DEFAULT_CHECKOUT_URI.toUri(),
    ): LinkType {
        val appLinkWillRoute = isMerchantDefaultHandler(appLinkReturnUri) &&
            (deviceInspector.canResolvePayPalAppSwitch() || getAppLinksCompatibleBrowserUseCase(checkoutUri))
        return if (appLinkWillRoute) LinkType.APP_LINK else LinkType.DEEP_LINK
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
