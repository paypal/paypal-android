@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

internal enum class VaultEvent(val value: String) {
    // @formatter:off
    STARTED(  "paypal-web-payments:vault-wo-purchase:started"),
    SUCCEEDED("paypal-web-payments:vault-wo-purchase:succeeded"),
    FAILED(   "paypal-web-payments:vault-wo-purchase:failed"),
    CANCELED( "paypal-web-payments:vault-wo-purchase:canceled"),

    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("paypal-web-payments:vault-wo-purchase:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED(   "paypal-web-payments:vault-wo-purchase:auth-challenge-presentation:failed"),
    // @formatter:on
}
