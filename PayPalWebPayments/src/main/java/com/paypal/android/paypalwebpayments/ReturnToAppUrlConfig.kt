package com.paypal.android.paypalwebpayments

/**
 * URL configuration for returning the buyer to the merchant app after the PayPal
 * checkout or vault flow.
 *
 * @property returnAppUrl URL the SDK uses to return the buyer to the merchant app after approval.
 * @property cancelAppUrl URL the SDK uses to return the buyer to the merchant app after cancellation.
 * @property fallbackSchemeUrl Custom URI scheme used as a deep-link fallback if the App Link
 *   return fails.
 */
data class ReturnToAppUrlConfig(
    val returnAppUrl: String,
    val cancelAppUrl: String,
    val fallbackSchemeUrl: String,
)
