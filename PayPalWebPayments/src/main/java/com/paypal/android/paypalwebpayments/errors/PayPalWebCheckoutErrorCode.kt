package com.paypal.android.paypalwebpayments.errors

internal enum class PayPalWebCheckoutErrorCode {
    UNKNOWN,
    MALFORMED_RESULT,
    BROWSER_SWITCH,
    NO_RETURN_TO_APP_STRATEGY,
    ORDER_CREATION_FAILED,
    SETUP_TOKEN_CREATION_FAILED,
}
