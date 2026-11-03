@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.venmo.analytics

internal enum class VenmoCheckoutEvent(val value: String) {
    // @formatter:off
    STARTED(  "venmo-payments:checkout:started"),
    SUCCEEDED("venmo-payments:checkout:succeeded"),
    FAILED(   "venmo-payments:checkout:failed"),
    CANCELED( "venmo-payments:checkout:canceled"),

    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("venmo-payments:checkout:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED(   "venmo-payments:checkout:auth-challenge-presentation:failed"),
    // @formatter:on
}
