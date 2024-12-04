package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PayPalSDKErrorCode {
    UNKNOWN,
    DATA_PARSING_ERROR,
    UNKNOWN_HOST,
    NO_RESPONSE_DATA,
    INVALID_URL_REQUEST,
    SERVER_RESPONSE_ERROR,
    CHECKOUT_ERROR,
    NATIVE_CHECKOUT_ERROR,
    GRAPHQL_JSON_INVALID_ERROR
}
