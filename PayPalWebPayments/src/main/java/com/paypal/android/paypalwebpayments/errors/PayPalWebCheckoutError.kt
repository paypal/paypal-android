package com.paypal.android.paypalwebpayments.errors

import com.paypal.android.corepayments.PayPalSDKError

internal object PayPalWebCheckoutError {

    // 0. An unknown error occurred.
    val unknownError = PayPalSDKError(
        code = Code.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support."
    )

    // 1. Result did not contain the expected data.
    val malformedResultError = PayPalSDKError(
        code = Code.MALFORMED_RESULT.ordinal,
        errorDescription = "Result did not contain the expected data. Payer ID or Order ID is null."
    )
}

internal enum class Code {
    UNKNOWN,
    MALFORMED_RESULT
}
