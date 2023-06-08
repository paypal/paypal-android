package com.paypal.android.corepayments

object APIClientError {

    // 0. An unknown error occurred.
    fun unknownError(correlationId: String?) = PayPalSDKError(
        code = Code.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 1. Error parsing HTTP response data.
    fun dataParsingError(correlationId: String?) = PayPalSDKError(
        code = Code.DATA_PARSING_ERROR.ordinal,
        errorDescription = "An error occurred parsing HTTP response data." +
                " Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 2. Unknown Host from network.
    fun unknownHost(correlationId: String?) = PayPalSDKError(
        code = Code.UNKNOWN_HOST.ordinal,
        errorDescription = "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 3. Missing HTTP response data.
    fun noResponseData(correlationId: String?) = PayPalSDKError(
        code = Code.NO_RESPONSE_DATA.ordinal,
        errorDescription = "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )

    // 4. There was an error constructing the URLRequest.
    val invalidUrlRequest = PayPalSDKError(
        code = Code.INVALID_URL_REQUEST.ordinal,
        errorDescription = "An error occurred constructing an HTTP request. Contact developer.paypal.com/support."
    )

    // 5. The server's response body returned an error message.
    fun serverResponseError(correlationId: String?) = PayPalSDKError(
        code = Code.SERVER_RESPONSE_ERROR.ordinal,
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
            code = Code.CHECKOUT_ERROR.ordinal,
            errorDescription = description
        )
    }

    fun clientIDNotFoundError(code: Int, correlationId: String?) = PayPalSDKError(
        code = code,
        errorDescription = "Error fetching clientId. Contact developer.paypal.com/support.",
        correlationId = correlationId
    )
}

enum class Code {
    UNKNOWN,
    DATA_PARSING_ERROR,
    UNKNOWN_HOST,
    NO_RESPONSE_DATA,
    INVALID_URL_REQUEST,
    SERVER_RESPONSE_ERROR,
    CHECKOUT_ERROR
}
