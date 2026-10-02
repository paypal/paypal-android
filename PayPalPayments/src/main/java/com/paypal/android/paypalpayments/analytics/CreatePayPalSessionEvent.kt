@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalpayments.analytics

/**
 * Fires from [com.paypal.android.paypalpayments.PayPalClient.createPayPalSession],
 * for both the checkout and vault flows, under the same event strings. [PayPalAnalytics]
 * distinguishes which flow fired the event via the `is_vault` param.
 */
internal enum class CreatePayPalSessionEvent(val value: String) {
    // @formatter:off
    STARTED(  "paypal-payments:create-paypal-session:started"),
    SUCCEEDED("paypal-payments:create-paypal-session:succeeded"),
    FAILED(   "paypal-payments:create-paypal-session:failed"),
    // @formatter:on
}
