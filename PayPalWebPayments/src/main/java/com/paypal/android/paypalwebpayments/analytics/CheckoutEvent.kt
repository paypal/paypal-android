@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

internal enum class CheckoutEvent(val value: String) {
    // @formatter:off
    STARTED("paypal-web-payments:checkout:started"),
    SUCCEEDED("paypal-web-payments:checkout:succeeded"),
    FAILED("paypal-web-payments:checkout:failed"),
    CANCELED( "paypal-web-payments:checkout:canceled"),

    SESSION_NOT_STARTED("paypal-web-payments:checkout:session-not-started"),

    APP_SWITCH_STARTED("paypal-web-payments:checkout:app-switch:started"),
    APP_SWITCH_SUCCEEDED("paypal-web-payments:checkout:app-switch:succeeded"),
    APP_SWITCH_FAILED("paypal-web-payments:checkout:app-switch:failed"),
    APP_SWITCH_CANCELED("paypal-web-payments:checkout:app-switch:canceled"),

    AUTH_CHALLENGE_PRESENTATION_STARTED("paypal-web-payments:checkout:auth-challenge-presentation:started"),
    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("paypal-web-payments:checkout:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED("paypal-web-payments:checkout:auth-challenge-presentation:failed"),
    AUTH_CHALLENGE_PRESENTATION_CANCELED("paypal-web-payments:checkout:auth-challenge-presentation:canceled"),

    HANDLE_RETURN_STARTED("paypal-web-payments:checkout:handle-return:started"),
    HANDLE_RETURN_SUCCEEDED("paypal-web-payments:checkout:handle-return:succeeded"),
    HANDLE_RETURN_FAILED("paypal-web-payments:checkout:handle-return:failed"),
    // @formatter:on
}
