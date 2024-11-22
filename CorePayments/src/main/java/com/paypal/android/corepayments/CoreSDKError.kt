package com.paypal.android.corepayments

object CoreSDKError {

    fun unknownHost(reason: Throwable) = PayPalSDKError(
        code = CoreSDKErrorCode.UNKNOWN_HOST.ordinal,
        errorDescription = "Unknown host.",
        reason = reason
    )

    fun illegalState(reason: Throwable) = PayPalSDKError(
        code = CoreSDKErrorCode.ILLEGAL_STATE.ordinal,
        errorDescription = "Unknown host.",
        reason = reason
    )

    fun unknown(reason: Throwable? = null) = PayPalSDKError(
        code = CoreSDKErrorCode.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred.",
        reason = reason
    )
}
