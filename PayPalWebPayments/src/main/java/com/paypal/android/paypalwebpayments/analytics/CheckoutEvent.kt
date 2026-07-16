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

    // Ref: https://paypal.atlassian.net/wiki/spaces/~7120203361479131b645799d3eacdd2de5b990/pages/2982842759
    SESSION_NOT_STARTED("paypal-web-payments:checkout:session-not-started"),

    // NOTE: matches iOS's existing `app-switch-open:*` string for SUCCEEDED/FAILED so both
    // platforms agree today. iOS also has a `app-switch-open:*` vs `app-switch:*` naming
    // inconsistency across its own events per the doc — flagged there as needing alignment
    // before shipping, independent of this Android change.
    APP_SWITCH_STARTED(  "paypal-web-payments:checkout:app-switch:started"),
    APP_SWITCH_SUCCEEDED("paypal-web-payments:checkout:app-switch-open:succeeded"),
    APP_SWITCH_FAILED(   "paypal-web-payments:checkout:app-switch-open:failed"),
    APP_SWITCH_CANCELED( "paypal-web-payments:checkout:app-switch:canceled"),

    BROWSER_PRESENTATION_STARTED("paypal-web-payments:checkout:browser-presentation:started"),

    HANDLE_RETURN_STARTED(  "paypal-web-payments:checkout:handle-return:started"),
    HANDLE_RETURN_SUCCEEDED("paypal-web-payments:checkout:handle-return:succeeded"),
    HANDLE_RETURN_FAILED(   "paypal-web-payments:checkout:handle-return:failed"),
    // @formatter:on
}
