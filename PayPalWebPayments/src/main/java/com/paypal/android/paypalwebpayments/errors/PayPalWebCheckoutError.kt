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

    // 3. createOrder callback returned a failure
    fun createOrderFailed(cause: Exception) = PayPalSDKError(
        code = PayPalWebCheckoutErrorCode.CREATE_ORDER_FAILED.ordinal,
        errorDescription = cause.message ?: "createOrder callback returned a failure."
    )

    // 4. createSetupToken callback returned a failure
    fun createSetupTokenFailed(cause: Exception) = PayPalSDKError(
        code = PayPalWebCheckoutErrorCode.CREATE_SETUP_TOKEN_FAILED.ordinal,
        errorDescription = cause.message ?: "createSetupToken callback returned a failure."
    )
}
