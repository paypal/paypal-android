@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

/**
 * Fires from [com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient.createPayPalSession],
 * for both the checkout and vault flows, under the same event strings. [PayPalWebAnalytics]
 * distinguishes which flow fired the event via the `is_vault_request` param.
 */
internal enum class CreatePayPalSessionEvent(val value: String) {
    // @formatter:off
    STARTED(  "paypal-web-payments:create-paypal-session:started"),
    SUCCEEDED("paypal-web-payments:create-paypal-session:succeeded"),
    FAILED(   "paypal-web-payments:create-paypal-session:failed"),
    // @formatter:on
}
