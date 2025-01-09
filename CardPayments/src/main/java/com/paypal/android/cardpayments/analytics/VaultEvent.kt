@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.cardpayments.analytics

internal enum class VaultEvent(val value: String) {
    // @formatter:off
    STARTED(  "card-payments:vault-wo-purchase:started"),
    SUCCEEDED("card-payments:vault-wo-purchase:succeeded"),
    FAILED(   "card-payments:vault-wo-purchase:failed"),

    AUTH_CHALLENGE_REQUIRED("card-payments:vault-wo-purchase:auth-challenge-required"),

    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("card-payments:vault-wo-purchase:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED(   "card-payments:vault-wo-purchase:auth-challenge-presentation:failed"),

    AUTH_CHALLENGE_SUCCEEDED("card-payments:vault-wo-purchase:auth-challenge:succeeded"),
    AUTH_CHALLENGE_FAILED(   "card-payments:vault-wo-purchase:auth-challenge:failed"),
    AUTH_CHALLENGE_CANCELED( "card-payments:vault-wo-purchase:auth-challenge:canceled"),
    // @formatter:on
}
