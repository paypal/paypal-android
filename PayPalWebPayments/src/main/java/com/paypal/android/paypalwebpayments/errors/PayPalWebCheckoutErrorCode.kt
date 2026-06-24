package com.paypal.android.paypalwebpayments.errors

internal enum class PayPalWebCheckoutErrorCode {
    UNKNOWN,
    MALFORMED_RESULT,
    BROWSER_SWITCH,
    CREATE_ORDER_FAILED,
    CREATE_SETUP_TOKEN_FAILED,
}
