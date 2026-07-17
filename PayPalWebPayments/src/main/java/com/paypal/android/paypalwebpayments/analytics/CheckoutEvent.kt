@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

internal enum class CheckoutEvent(val value: String) {
    // @formatter:off
    STARTED(  "paypal-web-payments:checkout:started"),
    SUCCEEDED("paypal-web-payments:checkout:succeeded"),
    FAILED(   "paypal-web-payments:checkout:failed"),
    CANCELED( "paypal-web-payments:checkout:canceled"),

    SESSION_NOT_STARTED("paypal-web-payments:checkout:session-not-started"),

    APP_SWITCH_STARTED(  "paypal-web-payments:checkout:app-switch:started"),
    APP_SWITCH_SUCCEEDED("paypal-web-payments:checkout:app-switch-open:succeeded"),
    APP_SWITCH_FAILED(   "paypal-web-payments:checkout:app-switch-open:failed"),
    APP_SWITCH_CANCELED( "paypal-web-payments:checkout:app-switch:canceled"),

    BROWSER_PRESENTATION_STARTED("paypal-web-payments:checkout:browser-presentation:started"),
    BROWSER_PRESENTATION_SUCCEEDED("paypal-web-payments:checkout:browser-presentation:succeeded"),
    BROWSER_PRESENTATION_FAILED("paypal-web-payments:checkout:browser-presentation:failed"),
    BROWSER_PRESENTATION_CANCELED("paypal-web-payments:checkout:browser-presentation:canceled"),

    HANDLE_RETURN_STARTED(  "paypal-web-payments:checkout:handle-return:started"),
    HANDLE_RETURN_SUCCEEDED("paypal-web-payments:checkout:handle-return:succeeded"),
    HANDLE_RETURN_FAILED(   "paypal-web-payments:checkout:handle-return:failed"),
    // @formatter:on
}
