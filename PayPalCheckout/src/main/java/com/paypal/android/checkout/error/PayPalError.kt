package com.paypal.android.checkout.error

import com.paypal.android.core.CoreSDKError

internal object PayPalError {

    // 0. An unknown error occurred.
    val unknownError = CoreSDKError(
        code = Code.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support."
    )

    // 1. Result did not contain the expected data.
    val malformedResultError = CoreSDKError(
        code = Code.MALFORMED_RESULT.ordinal,
        errorDescription = "Result did not contain the expected data. Payer ID or Order ID is null."
    )
}

internal enum class Code {
    UNKNOWN,
    MALFORMED_RESULT
}