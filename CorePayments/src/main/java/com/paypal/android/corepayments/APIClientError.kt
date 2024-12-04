package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object APIClientError {

    // 0. An unknown error occurred.
    fun unknownError(correlationId: String?) = PayPalSDKError(
        code = PayPalSDKErrorCode.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 1. Error parsing HTTP response data.
    fun dataParsingError(correlationId: String?) = PayPalSDKError(
        code = PayPalSDKErrorCode.DATA_PARSING_ERROR.ordinal,
        errorDescription = "An error occurred parsing HTTP response data." +
                " Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 2. Unknown Host from network.
    fun unknownHost(correlationId: String?) = PayPalSDKError(
        code = PayPalSDKErrorCode.UNKNOWN_HOST.ordinal,
        errorDescription = "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 3. Missing HTTP response data.
    fun noResponseData(correlationId: String?) = PayPalSDKError(
        code = PayPalSDKErrorCode.NO_RESPONSE_DATA.ordinal,
        errorDescription = "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 4. There was an error constructing the URLRequest.
    val invalidUrlRequest = PayPalSDKError(
        code = PayPalSDKErrorCode.INVALID_URL_REQUEST.ordinal,
        errorDescription = "An error occurred constructing an HTTP request. Contact developer.paypal.com/support."
    )

    // 5. The server's response body returned an error message.
    fun serverResponseError(correlationId: String?) = PayPalSDKError(
        code = PayPalSDKErrorCode.SERVER_RESPONSE_ERROR.ordinal,
        errorDescription = "A server error occurred. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // Error returned from HttpURLConnection while making request.
    fun httpURLConnectionError(code: Int, description: String, correlationId: String?) =
        PayPalSDKError(
            code = code,
            errorDescription = description,
            correlationId = correlationId
        )

    val payPalCheckoutError: (description: String) -> PayPalSDKError = { description ->
        PayPalSDKError(
            code = PayPalSDKErrorCode.CHECKOUT_ERROR.ordinal,
            errorDescription = description
        )
    }

    val payPalNativeCheckoutError: (description: String, reason: Exception) -> PayPalSDKError =
        { description, reason ->
            PayPalSDKError(
                code = PayPalSDKErrorCode.NATIVE_CHECKOUT_ERROR.ordinal,
                errorDescription = description,
                reason = reason
            )
        }

    fun clientIDNotFoundError(code: Int, correlationId: String?) = PayPalSDKError(
        code = code,
        errorDescription = "Error fetching clientId. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    fun graphQLJSONParseError(correlationId: String?, reason: Exception): PayPalSDKError {
        val message =
            "An error occurred while parsing the GraphQL response JSON. Contact developer.paypal.com/support."
        val error = PayPalSDKError(
            code = PayPalSDKErrorCode.GRAPHQL_JSON_INVALID_ERROR.ordinal,
            errorDescription = message,
            correlationId = correlationId,
            reason = reason
        )
        return error
    }
}
