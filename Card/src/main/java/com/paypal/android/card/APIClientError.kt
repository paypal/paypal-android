package com.paypal.android.card

object APIClientError {

    private const val domain = "APIClientErrorDomain"

    // 0. An unknown error occurred.
    val unknownError = PayPalSDKError(
        code = Code.UNKNOWN.ordinal,
        domain = domain,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support."
    )

    // 1. Error parsing HTTP response data.
    val dataParsingError = PayPalSDKError(
        code = Code.DATA_PARSING_ERROR.ordinal,
        domain = domain,
        errorDescription = "An error occurred parsing HTTP response data. Contact developer.paypal.com/support."
    )

    // 2. Unknown Host from network.
    val unknownHost = PayPalSDKError(
        code = Code.UNKNOWN_HOST.ordinal,
        domain = domain,
        errorDescription = "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support."
    )

    // 3. Missing HTTP response data.
    val noResponseData = PayPalSDKError(
        code = Code.NO_RESPONSE_DATA.ordinal,
        domain = domain,
        errorDescription = "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support."
    )

    // 4. There was an error constructing the URLRequest.
    val invalidUrlRequest = PayPalSDKError(
        code = Code.INVALID_URL_REQUEST.ordinal,
        domain = domain,
        errorDescription = "An error occurred constructing an HTTP request. Contact developer.paypal.com/support."
    )

    // 5. The server's response body returned an error message.
    val serverResponseError = PayPalSDKError(
        code = Code.SERVER_RESPONSE_ERROR.ordinal,
        domain = domain,
        errorDescription = "A server occurred. Contact developer.paypal.com/support."
    )


    // Error returned from HttpURLConnection while making request.
    val httpURLConnectionError: (code: Int, description: String) -> PayPalSDKError = { code, description ->
        PayPalSDKError(
            code = code,
            domain = domain,
            errorDescription = description
        )
    }

}

internal enum class Code {
    UNKNOWN,
    DATA_PARSING_ERROR,
    UNKNOWN_HOST,
    NO_RESPONSE_DATA,
    INVALID_URL_REQUEST,
    SERVER_RESPONSE_ERROR
}