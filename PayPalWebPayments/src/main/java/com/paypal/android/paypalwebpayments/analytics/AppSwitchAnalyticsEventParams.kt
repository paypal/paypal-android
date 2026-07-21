package com.paypal.android.paypalwebpayments.analytics

/**
 * Bundles every value [PayPalWebAnalytics.notify] needs to log an analytics event, so it can be
 * threaded through as a single argument instead of a long list of individual parameters.
 */
internal data class AppSwitchAnalyticsEventParams(
    val checkoutOrderId: String? = null,
    val vaultSetupTokenId: String? = null,
    val shopperSessionId: String? = null,
    val isCachedSession: Boolean? = null,
    val shopperSessionExpiration: String? = null,
    val matchedAuthenticationMethods: List<String>? = null,
    val appSwitchEligible: Boolean? = null,
    val ineligibleReason: String? = null,
    val fallbackUrl: String? = null,
    val userActionValue: String? = null,
    val appSwitchEnabled: Boolean = false,
    val appSwitchUrl: String? = null,
    val isVault: Boolean? = null,
    val merchantId: String? = null,
    val bnCode: String? = null,
    val clientId: String? = null,
    val paypalInstalled: String? = null,
    val returnAppUrl: String? = null,
    val cancelAppUrl: String? = null,
    val fallbackSchemeUrl: String? = null,
)
