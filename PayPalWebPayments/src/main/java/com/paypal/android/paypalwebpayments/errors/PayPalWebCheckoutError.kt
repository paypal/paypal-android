package com.paypal.android.paypalwebpayments.errors

import com.paypal.android.corepayments.PayPalSDKError

internal object PayPalWebCheckoutError {

    // 0. An unknown error occurred.
    val unknownError = PayPalSDKError(
        code = PayPalWebCheckoutErrorCode.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support."
    )

    // 1. Result did not contain the expected data.
    val malformedResultError = PayPalSDKError(
        code = PayPalWebCheckoutErrorCode.MALFORMED_RESULT.ordinal,
        errorDescription = "Result did not contain the expected data. Payer ID or Order ID is null."
    )

    // 2. An error occurred while browser switching
    fun browserSwitchError(cause: Exception) = PayPalSDKError(
        code = PayPalWebCheckoutErrorCode.BROWSER_SWITCH.ordinal,
        errorDescription = cause.message ?: "Unable to Browser Switch"
    )

    // 3. ReturnToAppStrategy or urlScheme is required
    val noReturnToAppStrategyError = PayPalSDKError(
        code = PayPalWebCheckoutErrorCode.NO_RETURN_TO_APP_STRATEGY.ordinal,
        errorDescription = "ReturnToAppStrategy or urlScheme is required. "
    )
}
