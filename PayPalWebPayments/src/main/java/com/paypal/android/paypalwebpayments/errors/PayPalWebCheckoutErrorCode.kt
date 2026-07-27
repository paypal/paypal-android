package com.paypal.android.paypalwebpayments.errors

internal enum class PayPalWebCheckoutErrorCode {
    UNKNOWN,
    MALFORMED_RESULT,
    BROWSER_SWITCH,
    NO_RETURN_TO_APP_STRATEGY,
    SESSION_NOT_CREATED,
    RETURN_TO_APP_URL_CONFIG_MISSING,
}
