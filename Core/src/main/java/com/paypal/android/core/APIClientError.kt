package com.paypal.android.core

object APIClientError {

    // 0. An unknown error occurred.
    val unknownError = CoreSDKError(
        code = Code.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support."
    )

    // 1. Error parsing HTTP response data.
    val dataParsingError = CoreSDKError(
        code = Code.DATA_PARSING_ERROR.ordinal,
        errorDescription = "An error occurred parsing HTTP response data. Contact developer.paypal.com/support."
    )

    // 2. Unknown Host from network.
    val unknownHost = CoreSDKError(
        code = Code.UNKNOWN_HOST.ordinal,
        errorDescription = "An error occurred due to an invalid HTTP response. Contact developer.paypal.com/support."
    )

    // 3. Missing HTTP response data.
    val noResponseData = CoreSDKError(
        code = Code.NO_RESPONSE_DATA.ordinal,
        errorDescription = "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support."
    )

    // 4. There was an error constructing the URLRequest.
    val invalidUrlRequest = CoreSDKError(
        code = Code.INVALID_URL_REQUEST.ordinal,
        errorDescription = "An error occurred constructing an HTTP request. Contact developer.paypal.com/support."
    )

    // 5. The server's response body returned an error message.
    val serverResponseError = CoreSDKError(
        code = Code.SERVER_RESPONSE_ERROR.ordinal,
        errorDescription = "A server occurred. Contact developer.paypal.com/support."
    )

    // Error returned from HttpURLConnection while making request.
    val HTTP_URL_CONNECTION_ERROR: (code: Int, description: String) -> CoreSDKError = { code, description ->
        CoreSDKError(
            code = code,
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
