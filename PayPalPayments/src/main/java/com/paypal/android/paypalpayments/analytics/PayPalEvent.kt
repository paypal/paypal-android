@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalpayments.analytics

internal enum class PayPalEvent(val value: String) {
    // @formatter:off
    STARTED("paypal-payments:checkout:started"),
    SUCCEEDED("paypal-payments:checkout:succeeded"),
    FAILED("paypal-payments:checkout:failed"),
    CANCELED( "paypal-payments:checkout:canceled"),

    SESSION_NOT_STARTED("paypal-payments:checkout:session-not-started"),

    APP_SWITCH_STARTED("paypal-payments:checkout:app-switch:started"),
    APP_SWITCH_SUCCEEDED("paypal-payments:checkout:app-switch:succeeded"),
    APP_SWITCH_FAILED("paypal-payments:checkout:app-switch:failed"),

    AUTH_CHALLENGE_PRESENTATION_STARTED("paypal-payments:checkout:auth-challenge-presentation:started"),
    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("paypal-payments:checkout:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED("paypal-payments:checkout:auth-challenge-presentation:failed"),

    HANDLE_RETURN_STARTED("paypal-payments:checkout:handle-return:started"),
    HANDLE_RETURN_SUCCEEDED("paypal-payments:checkout:handle-return:succeeded"),
    HANDLE_RETURN_FAILED("paypal-payments:checkout:handle-return:failed"),
    // @formatter:on
}
