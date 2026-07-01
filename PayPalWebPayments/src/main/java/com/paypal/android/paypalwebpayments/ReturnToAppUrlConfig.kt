package com.paypal.android.paypalwebpayments

/**
 * URL configuration for returning the shopper back to the merchant app after checkout.
 *
 * Passed to [PayPalWebCheckoutClient.createPayPalSession] and stored internally by the SDK.
 *
 * @param returnAppUrl The deep link URL to open when checkout completes successfully.
 * @param cancelAppUrl The deep link URL to open when the shopper cancels checkout.
 * @param fallbackSchemeUrl A fallback URL scheme used when the primary deep link cannot be resolved.
 */
data class ReturnToAppUrlConfig(
    val returnAppUrl: String,
    val cancelAppUrl: String,
    val fallbackSchemeUrl: String,
)
