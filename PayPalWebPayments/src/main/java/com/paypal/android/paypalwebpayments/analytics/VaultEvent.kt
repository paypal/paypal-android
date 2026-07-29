@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

internal enum class VaultEvent(val value: String) {
    // @formatter:off
    STARTED("paypal-web-payments:vault-wo-purchase:started"),
    SUCCEEDED("paypal-web-payments:vault-wo-purchase:succeeded"),
    FAILED("paypal-web-payments:vault-wo-purchase:failed"),
    CANCELED("paypal-web-payments:vault-wo-purchase:canceled"),

    SESSION_NOT_STARTED("paypal-web-payments:vault-wo-purchase:session-not-started"),

    APP_SWITCH_STARTED("paypal-web-payments:vault-wo-purchase:app-switch:started"),
    APP_SWITCH_SUCCEEDED("paypal-web-payments:vault-wo-purchase:app-switch:succeeded"),
    APP_SWITCH_FAILED("paypal-web-payments:vault-wo-purchase:app-switch:failed"),
    APP_SWITCH_CANCELED("paypal-web-payments:vault-wo-purchase:app-switch:canceled"),

    AUTH_CHALLENGE_PRESENTATION_STARTED("paypal-web-payments:vault-wo-purchase:auth-challenge-presentation:started"),
    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("paypal-web-payments:vault-wo-purchase:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED("paypal-web-payments:vault-wo-purchase:auth-challenge-presentation:failed"),
    AUTH_CHALLENGE_PRESENTATION_CANCELED("paypal-web-payments:vault-wo-purchase:auth-challenge-presentation:canceled"),

    HANDLE_RETURN_STARTED("paypal-web-payments:vault-wo-purchase:handle-return:started"),
    HANDLE_RETURN_SUCCEEDED("paypal-web-payments:vault-wo-purchase:handle-return:succeeded"),
    HANDLE_RETURN_FAILED("paypal-web-payments:vault-wo-purchase:handle-return:failed"),
    // @formatter:on
}
