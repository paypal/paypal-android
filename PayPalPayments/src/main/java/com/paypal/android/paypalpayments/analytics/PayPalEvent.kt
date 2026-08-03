@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalpayments.analytics

internal enum class PayPalEvent(val value: String) {
    // @formatter:off
    STARTED("pay-pal-payments:checkout:started"),
    SUCCEEDED("pay-pal-payments:checkout:succeeded"),
    FAILED("pay-pal-payments:checkout:failed"),
    CANCELED( "pay-pal-payments:checkout:canceled"),

    SESSION_NOT_STARTED("pay-pal-payments:checkout:session-not-started"),

    APP_SWITCH_STARTED("pay-pal-payments:checkout:app-switch:started"),
    APP_SWITCH_SUCCEEDED("pay-pal-payments:checkout:app-switch:succeeded"),
    APP_SWITCH_FAILED("pay-pal-payments:checkout:app-switch:failed"),

    AUTH_CHALLENGE_PRESENTATION_STARTED("pay-pal-payments:checkout:auth-challenge-presentation:started"),
    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("pay-pal-payments:checkout:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED("pay-pal-payments:checkout:auth-challenge-presentation:failed"),

    HANDLE_RETURN_STARTED("pay-pal-payments:checkout:handle-return:started"),
    HANDLE_RETURN_SUCCEEDED("pay-pal-payments:checkout:handle-return:succeeded"),
    HANDLE_RETURN_FAILED("pay-pal-payments:checkout:handle-return:failed"),
    // @formatter:on
}
