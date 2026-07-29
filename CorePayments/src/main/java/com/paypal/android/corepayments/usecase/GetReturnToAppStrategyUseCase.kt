package com.paypal.android.corepayments.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.common.DeviceInspector

/**
 * Decides how to return to the merchant app after a checkout/vault flow, returning the concrete
 * [ReturnToAppStrategy] to use, wrapped in a [GetReturnToAppStrategyResult].
 *
 * Returns [GetReturnToAppStrategyResult.Success] when the merchant app is the verified default handler for its
 * own App Link return URL AND either the first-party PayPal app or an App-Links-compatible browser
 * will honor the return. Also returns [ReturnToAppStrategy.AppLink] when no custom URL scheme fallback
 * is available, since there is nothing to deep-link to. Otherwise returns
 * [ReturnToAppStrategy.CustomUrlScheme], signalling that the custom URL scheme fallback should be used.
 *
 * Returns [GetReturnToAppStrategyResult.Failure] when both [appLinkReturnUrl] and [fallbackSchemeUrl]
 * are blank, since there would be no way to return to the merchant app at all.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnToAppStrategyUseCase(
    private val applicationContext: Context,
    private val deviceInspector: DeviceInspector,
) {

    /**
     * Builds a [GetReturnToAppStrategyUseCase] wired with its collaborators from an application
     * [Context], following the SDK's manual dependency-injection convention.
     */
    constructor(applicationContext: Context) : this(
        applicationContext = applicationContext,
        deviceInspector = DeviceInspector(applicationContext),
    )

    /**
     * @param appLinkReturnUrl The merchant's App Link return URL (`returnAppUrl`). Blank is treated
     *   as "no App Link", so the return can only ever be a deep link (or App Link if there is also
     *   no fallback scheme).
     * @param fallbackSchemeUrl The merchant's custom URL scheme fallback. When blank there is no
     *   scheme to deep-link to, so [ReturnToAppStrategy.AppLink] is returned regardless of App Link
     *   routing.
     * @param checkoutUri The checkout URL used to probe whether the default browser is
     *   App-Links-compatible. Defaults to PayPal's checkout URL.
     *
     * @return [GetReturnToAppStrategyResult.Failure] when both [appLinkReturnUrl] and
     *   [fallbackSchemeUrl] are blank, since there would be no usable return route back to the
     *   merchant app.
     */
    operator fun invoke(
        appLinkReturnUrl: String,
        fallbackSchemeUrl: String,
        checkoutUri: Uri = DEFAULT_BROWSER_PROBE_URI,
    ): GetReturnToAppStrategyResult {
        if (appLinkReturnUrl.isBlank() && fallbackSchemeUrl.isBlank()) {
            return GetReturnToAppStrategyResult.Failure(
                "Either appLinkReturnUrl or fallbackSchemeUrl must be provided to return to the merchant app."
            )
        }

        val returnToAppStrategy = if (fallbackSchemeUrl.isBlank()) {
            // Without a custom URL scheme there is nothing to deep-link to, so App Link is the only
            // possible return type. Short-circuit before doing any package-manager probing.
            ReturnToAppStrategy.AppLink(appLinkReturnUrl)
        } else {
            val shouldRouteToAppLink = isMerchantDefaultHandlerForReturnUrl(appLinkReturnUrl) &&
                canLaunchIntoAppLinkAwareTarget(checkoutUri)

            if (shouldRouteToAppLink) {
                ReturnToAppStrategy.AppLink(appLinkReturnUrl)
            } else {
                ReturnToAppStrategy.CustomUrlScheme(fallbackSchemeUrl)
            }
        }
        return GetReturnToAppStrategyResult.Success(returnToAppStrategy)
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
    private fun isMerchantDefaultHandlerForReturnUrl(appLinkReturnUrl: String): Boolean {
        val appLinkReturnUri = appLinkReturnUrl.takeIf { it.isNotBlank() }?.toUri()
        return appLinkReturnUri != null && applicationContext.packageName == getDefaultApp(appLinkReturnUri)
    }

    /**
     * True when the resolved default handler for [browserUri] is a browser known to honor Android
     * App Links, based on a static list of pre-tested browsers.
     */
    private fun hasAppLinksCompatibleBrowser(browserUri: Uri?): Boolean {
        val defaultApp = getDefaultApp(browserUri) ?: return false
        return APP_LINK_COMPATIBLE_BROWSERS.any { defaultApp.contains(it) }
    }

    /**
     * Returns the package name of the default application that handles [uri], or `null` if none can
     * be resolved.
     */
    private fun getDefaultApp(uri: Uri?): String? {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolveInfo = applicationContext.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private companion object {
        // A neutral https URL used only to probe the device's default browser: its default handler
        // reveals whether an App Link return would route. This must not be an app-link-verified path
        // for any PayPal-owned app, or it would resolve to that app instead of a browser and defeat
        // the probe. paypal.com is verified domain-wide (assetlinks.json grants "handle_all_urls" with
        // no path scoping) for the PayPal app, so it can't be used here. example.com is guaranteed to
        // have no first-party app link claim.
        private val DEFAULT_BROWSER_PROBE_URI = "https://example.com/checkout".toUri()

        // Pre-tested browsers for app links compatibility.
        private val APP_LINK_COMPATIBLE_BROWSERS = listOf(
            "com.android.chrome",
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
        )
    }
}
