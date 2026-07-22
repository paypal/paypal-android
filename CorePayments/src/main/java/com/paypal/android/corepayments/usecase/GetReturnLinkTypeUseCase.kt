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
 * browser will honor the return. Also returns [LinkType.APP_LINK] when no custom URL scheme fallback
 * is available, since there is nothing to deep-link to. Otherwise returns [LinkType.DEEP_LINK],
 * signalling that the custom URL scheme fallback should be used instead.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnLinkTypeUseCase(
    private val applicationContext: Context,
    private val deviceInspector: DeviceInspector,
    private val getDefaultApp: GetDefaultAppUseCase,
    private val hasAppLinksCompatibleBrowser: HasAppLinksCompatibleBrowserUseCase,
) {

    /**
     * @param appLinkReturnUrl The merchant's App Link return URL (`returnAppUrl`). Null or blank is
     *   treated as "no App Link", so the return can only ever be a deep link (or App Link if there is
     *   also no fallback scheme).
     * @param fallbackSchemeUrl The merchant's custom URL scheme fallback. When null or blank there is
     *   no scheme to deep-link to, so [LinkType.APP_LINK] is returned regardless of App Link routing.
     * @param checkoutUri The checkout URL used to probe whether the default browser is
     *   App-Links-compatible. Defaults to PayPal's checkout URL.
     */
    operator fun invoke(
        appLinkReturnUrl: String?,
        fallbackSchemeUrl: String?,
        checkoutUri: Uri = DEFAULT_CHECKOUT_URI,
    ): LinkType {
        // Without a custom URL scheme there is nothing to deep-link to, so App Link is the only
        // possible return type. Short-circuit before doing any package-manager probing.
        if (fallbackSchemeUrl.isNullOrBlank()) {
            return LinkType.APP_LINK
        }

        val shouldRouteToAppLink = isMerchantDefaultHandlerForReturnUrl(appLinkReturnUrl) &&
            canLaunchIntoAppLinkAwareTarget(checkoutUri)

        return if (shouldRouteToAppLink) {
            LinkType.APP_LINK
        } else {
            LinkType.DEEP_LINK
        }
    }

    /**
     * True when the checkout can be launched into a target that supports an Android App Link return:
     * the first-party PayPal app (reachable via app switch, which returns to the merchant via App
     * Link) or a default browser known to honor App Links.
     *
     * This reflects the outbound launch path. For the PayPal-app case it only verifies that we can
     * switch to the app and assumes that flow returns via App Link — it does not check the return leg.
     */
    private fun canLaunchIntoAppLinkAwareTarget(checkoutUri: Uri): Boolean =
        deviceInspector.canResolvePayPalAppSwitch() || hasAppLinksCompatibleBrowser(checkoutUri)

    /**
     * True when the merchant app is the default handler for its own App Link return URI. Returns
     * false when the shopper has unchecked "Open supported links" for the merchant app in Android
     * settings, or when App Link (Digital Asset Links) verification isn't configured.
     */
    private fun isMerchantDefaultHandlerForReturnUrl(appLinkReturnUrl: String?): Boolean {
        val appLinkReturnUri = appLinkReturnUrl?.takeIf { it.isNotBlank() }?.toUri()
        return appLinkReturnUri != null && applicationContext.packageName == getDefaultApp(appLinkReturnUri)
    }

    private companion object {
        // A neutral https URL used only to probe the device's default browser: its default handler
        // reveals whether an App Link return would route. This must not be an app-link-verified path
        // for any PayPal-owned app, or it would resolve to that app instead of a browser and defeat
        // the probe.
        private val DEFAULT_CHECKOUT_URI = "https://www.paypal.com/checkout".toUri()
    }
}
