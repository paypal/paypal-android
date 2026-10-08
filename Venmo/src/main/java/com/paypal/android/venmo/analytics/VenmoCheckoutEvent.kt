@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.venmo.analytics

internal enum class VenmoCheckoutEvent(val value: String) {
    // @formatter:off
    START(            "venmo:checkout:start"),
    SUCCESS(          "venmo:checkout:success"),
    FAIL(             "venmo:checkout:fail"),
    CANCELED(           "venmo:checkout:canceled"),

    LAUNCH_SUCCESS("venmo:checkout:launched:success"),
    LAUNCH_FAILED(   "venmo:checkout:launched:failed"),
    // @formatter:on
}
