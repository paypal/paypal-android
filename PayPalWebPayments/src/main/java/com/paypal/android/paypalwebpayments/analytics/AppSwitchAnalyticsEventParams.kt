package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityResponse

/**
 * Bundles every value [PayPalWebAnalytics.notify] needs to log an analytics event, so it can be
 * threaded through as a single argument instead of a long list of individual parameters.
 */
internal data class AppSwitchAnalyticsEventParams(

    /**
     * The id being approved — an order id for a checkout flow (set by
     * [PayPalWebCheckoutClient.start]) or a setup token id for a vault flow (set by
     * [PayPalWebCheckoutClient.vault]). Which one it is can be determined from [isVault].
     */
    val orderId: String? = null,

    /**
     * The shopper session returned by [PayPalWebCheckoutClient.createPayPalSession].
     */
    val shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse? = null,

    /**
     * Whether the shopper session was resolved from an existing PayPal session
     * ([PayPalUserIdentity.existingPayPalSessionId]) rather than freshly created.
     */
    val isCachedSession: Boolean? = null,

    /**
     * The call-to-action label requested for the PayPal checkout page, from [PayPalUserAction].
     */
    val userActionValue: String? = null,

    /**
     * Whether an app switch (as opposed to a browser/webview auth challenge) was actually attempted
     * for the current launch. Determines whether `APP_SWITCH_*` or `AUTH_CHALLENGE_PRESENTATION_*`
     * events are logged for the launch and its outcome.
     */
    val appSwitchEnabled: Boolean = false,

    /**
     * The URL used to attempt the app switch launch. Only set when [appSwitchEnabled] is true.
     */
    val appSwitchUrl: String? = null,

    /**
     * Whether this is a vault (setup token) flow rather than a checkout (order) flow.
     */
    val isVault: Boolean? = null,

    /**
     * Merchant id from [com.paypal.android.corepayments.CoreConfig]. Constant for the lifetime of
     * the owning [PayPalWebCheckoutClient] instance — preserved across [reset].
     */
    val merchantId: String? = null,

    /**
     * BN code from [com.paypal.android.corepayments.CoreConfig]. Constant for the lifetime of the
     * owning [PayPalWebCheckoutClient] instance — preserved across [reset].
     */
    val bnCode: String? = null,

    /**
     * Client id from [com.paypal.android.corepayments.CoreConfig]. Constant for the lifetime of the
     * owning [PayPalWebCheckoutClient] instance — preserved across [reset].
     */
    val clientId: String? = null,

    /**
     * Whether the PayPal native app is installed and able to handle an app switch. See
     * [PayPalWebCheckoutClient.canAttemptPayPalAppSwitch].
     */
    val paypalInstalled: String? = null,

    /**
     * App link URL used to return to the merchant app after checkout/vault completes.
     */
    val returnAppUrl: String? = null,

    /**
     * App link URL used to return to the merchant app after checkout/vault is canceled.
     */
    val cancelAppUrl: String? = null,

    /**
     * Custom URL scheme used as a deep-link fallback when returning via app link isn't available.
     */
    val fallbackSchemeUrl: String? = null,
)
