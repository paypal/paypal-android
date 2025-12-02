package com.paypal.android.paypalwebpayments

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderId The ID of the order to be approved.
 * @param fundingSource specify funding (credit, paylater or default)
 * @param appSwitchWhenEligible whether to switch to the PayPal app when eligible
 * @param appLinkUrl The app link URL to use for browser switch, or null to use default.
 *   Example values: "$myAppScheme://$myAppHost/$myPath", "https://$myDomain/$myPath"
 * @param fallbackUrlScheme The fallback custom URL scheme to use when app link is not configured properly.
 */
data class PayPalWebCheckoutRequest @JvmOverloads constructor(
    val orderId: String,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL,
    val appSwitchWhenEligible: Boolean = false,
    val appLinkUrl: String? = null,
    val fallbackUrlScheme: String? = null
)
