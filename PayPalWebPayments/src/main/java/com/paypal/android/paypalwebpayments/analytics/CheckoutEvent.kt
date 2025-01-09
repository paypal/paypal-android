@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

internal enum class CheckoutEvent(val value: String) {
    // @formatter:off
    STARTED(  "paypal-web-payments:checkout:started"),
    SUCCEEDED("paypal-web-payments:checkout:succeeded"),
    FAILED(   "paypal-web-payments:checkout:failed"),
    CANCELED( "paypal-web-payments:checkout:canceled"),

    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("paypal-web-payments:checkout:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED(   "paypal-web-payments:checkout:auth-challenge-presentation:failed"),
    // @formatter:on
}
