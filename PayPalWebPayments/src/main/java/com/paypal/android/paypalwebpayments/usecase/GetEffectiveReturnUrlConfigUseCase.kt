package com.paypal.android.paypalwebpayments.usecase

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.LinkType
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.returnUrl
import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig

/**
 * Resolves the [ReturnToAppUrlConfig] to send to the server for the given [LinkType], which the
 * caller obtains from [com.paypal.android.corepayments.usecase.GetReturnLinkTypeUseCase].
 *
 * - [LinkType.APP_LINK]: the config is returned unchanged, so the merchant's https
 *   [ReturnToAppUrlConfig.returnAppUrl] / [ReturnToAppUrlConfig.cancelAppUrl] are used.
 * - [LinkType.DEEP_LINK]: a copy with custom-scheme return/cancel URLs derived from
 *   [ReturnToAppUrlConfig.fallbackSchemeUrl], so PayPal redirects via the scheme the client
 *   captures. The success URL reuses [ReturnToAppStrategy.CustomUrlScheme]'s convention
 *   (`scheme://x-callback-url/paypal-sdk/paypal-checkout`); the cancel URL appends a `cancel` path
 *   segment, which both return parsers detect (checkout: no PayerID on cancel; vault: path contains
 *   "cancel"). [ReturnToAppUrlConfig.fallbackSchemeUrl] is preserved.
 *
 * Sending the chosen return URL to the server before the payment call mirrors the Braintree flow —
 * the client's device-only decision can't be made server-side, so it must be communicated through
 * these URLs.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetEffectiveReturnUrlConfigUseCase {

    operator fun invoke(
        urlConfig: ReturnToAppUrlConfig,
        linkType: LinkType
    ): ReturnToAppUrlConfig {
        if (urlConfig.fallbackSchemeUrl.isNullOrBlank()) {
            return urlConfig
        }

        return when (linkType) {
            LinkType.DEEP_LINK -> {
                val scheme = urlConfig.fallbackSchemeUrl
                val successUrl = ReturnToAppStrategy.CustomUrlScheme(scheme).returnUrl
                urlConfig.copy(returnAppUrl = successUrl, cancelAppUrl = "$successUrl/cancel")
            }

            LinkType.APP_LINK -> urlConfig
        }
    }
}
