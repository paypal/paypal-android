package com.paypal.android.paypalwebpayments.usecase

import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.paypal.android.corepayments.LinkType
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.returnUrl
import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig

/**
 * Resolves the [ReturnToAppUrlConfig] to send to the server for the given [LinkType], which the
 * caller obtains from [com.paypal.android.corepayments.usecase.GetReturnToAppStrategyUseCase].
 *
 * - [LinkType.APP_LINK]: the config is returned unchanged, so the merchant's https
 *   [ReturnToAppUrlConfig.returnAppUrl] / [ReturnToAppUrlConfig.cancelAppUrl] are used.
 * - [LinkType.DEEP_LINK]: a copy with custom-scheme return/cancel URLs derived from
 *   [ReturnToAppUrlConfig.fallbackSchemeUrl], so PayPal redirects via the scheme the client
 *   captures. The success URL reuses [ReturnToAppStrategy.CustomUrlScheme]'s convention
 *   (`scheme://x-callback-url/paypal-sdk/paypal-checkout`); the cancel URL appends a `cancel` path
 *   segment, which both return parsers detect (checkout: no PayerID on cancel; vault: path contains
 *   "cancel"). Any query params the merchant set on [ReturnToAppUrlConfig.returnAppUrl] /
 *   [ReturnToAppUrlConfig.cancelAppUrl] are preserved by appending them to the corresponding
 *   custom-scheme URL. [ReturnToAppUrlConfig.fallbackSchemeUrl] is preserved.
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
                val successBaseUrl = ReturnToAppStrategy.CustomUrlScheme(scheme).returnUrl
                urlConfig.copy(
                    returnAppUrl = successBaseUrl.withQueryFrom(urlConfig.returnAppUrl),
                    cancelAppUrl = "$successBaseUrl/cancel".withQueryFrom(urlConfig.cancelAppUrl),
                )
            }

            LinkType.APP_LINK -> urlConfig
        }
    }

    /**
     * Appends the query string from [originalUrl], if any, so merchant-supplied query params on the
     * original https return/cancel URL carry over to the custom-scheme URL.
     */
    private fun String.withQueryFrom(originalUrl: String): String {
        val query = originalUrl.toUri().query
        return if (query.isNullOrBlank()) this else "$this?$query"
    }
}
