package com.paypal.android.venmo.errors

import com.paypal.android.corepayments.PayPalSDKError

internal object VenmoError {

    // 1. Result did not contain the expected data.
    val malformedResultError = PayPalSDKError(
        code = VenmoErrorCode.MALFORMED_RESULT.ordinal,
        errorDescription = "Result did not contain the expected data. Order ID or Payer ID is null."
    )

    // 2. Result did not contain a valid approval or cancellation status.
    val invalidResultStatusError = PayPalSDKError(
        code = VenmoErrorCode.MALFORMED_RESULT.ordinal,
        errorDescription = "Result did not contain valid approval or cancellation status."
    )
}
