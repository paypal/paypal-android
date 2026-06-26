package com.paypal.android.paypalwebpayments

/**
 * URL configuration for returning the buyer back to the merchant app after checkout.
 *
 * @property returnAppUrl App Link URL to open when the buyer approves the payment.
 * @property cancelAppUrl App Link URL to open when the buyer cancels checkout.
 * @property fallbackSchemeUrl Custom URL scheme deep link used as a fallback if the App Link
 * return fails.
 */
data class PayPalURLConfig(
    val returnAppUrl: String,
    val cancelAppUrl: String,
    val fallbackSchemeUrl: String
)
