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

    // Shopper Session ID events (v3)
    CREATE_PAYPAL_SESSION_START(   "paypal-web-payments:checkout:create-paypal-session:start"),
    CREATE_PAYPAL_SESSION_SUCCESS( "paypal-web-payments:checkout:create-paypal-session:success"),
    CREATE_PAYPAL_SESSION_FAILURE( "paypal-web-payments:checkout:create-paypal-session:failure"),
    // @formatter:on
}
