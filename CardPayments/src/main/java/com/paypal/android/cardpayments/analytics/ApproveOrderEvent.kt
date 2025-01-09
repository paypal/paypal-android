@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.cardpayments.analytics

internal enum class ApproveOrderEvent(val value: String) {
    // @formatter:off
    STARTED(  "card-payments:approve-order:started"),
    SUCCEEDED("card-payments:approve-order:succeeded"),
    FAILED(   "card-payments:approve-order:failed"),

    AUTH_CHALLENGE_REQUIRED("card-payments:approve-order:auth-challenge-required"),

    AUTH_CHALLENGE_PRESENTATION_SUCCEEDED("card-payments:approve-order:auth-challenge-presentation:succeeded"),
    AUTH_CHALLENGE_PRESENTATION_FAILED(   "card-payments:approve-order:auth-challenge-presentation:failed"),

    AUTH_CHALLENGE_SUCCEEDED("card-payments:approve-order:auth-challenge:succeeded"),
    AUTH_CHALLENGE_FAILED(   "card-payments:approve-order:auth-challenge:failed"),
    AUTH_CHALLENGE_CANCELED( "card-payments:approve-order:auth-challenge:canceled"),
    // @formatter:on
}
