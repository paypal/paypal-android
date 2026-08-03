package com.paypal.android.paypalpayments.errors

internal enum class PayPalCheckoutErrorCode {
    UNKNOWN,
    MALFORMED_RESULT,
    BROWSER_SWITCH,
    NO_RETURN_TO_APP_STRATEGY,
    SESSION_NOT_CREATED,
    SESSION_CREATION_FAILED,
    RETURN_TO_APP_URL_CONFIG_MISSING,
}
