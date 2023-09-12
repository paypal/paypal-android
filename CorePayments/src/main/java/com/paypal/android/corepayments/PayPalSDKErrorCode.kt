package com.paypal.android.corepayments

internal enum class PayPalSDKErrorCode {
    UNKNOWN,
    DATA_PARSING_ERROR,
    UNKNOWN_HOST,
    NO_RESPONSE_DATA,
    INVALID_URL_REQUEST,
    SERVER_RESPONSE_ERROR,
    CHECKOUT_ERROR,
    NATIVE_CHECKOUT_ERROR
}
