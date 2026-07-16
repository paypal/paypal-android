@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

/**
 * Fires from [com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient.createPayPalSession],
 * for both the checkout and vault flows, under the same event strings. [PayPalWebAnalytics]
 * distinguishes which flow fired the event via the `is_vault_request` param.
 *
 * Ref: https://paypal.atlassian.net/wiki/spaces/~7120203361479131b645799d3eacdd2de5b990/pages/2982842759
 */
internal enum class CreatePayPalSessionEvent(val value: String) {
    // @formatter:off
    STARTED(  "paypal-web-payments:checkout:ssid-session:started"),
    SUCCEEDED("paypal-web-payments:create-paypal-session:succeeded"),
    FAILED(   "paypal-web-payments:create-paypal-session:failed"),
    // @formatter:on
}
