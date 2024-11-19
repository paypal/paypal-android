package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

internal object CardError {

    // 0. An error from 3DS verification
    val threeDSVerificationError = PayPalSDKError(
        code = CardErrorCode.THREEDS_VERIFICATION_FAILED.ordinal,
        errorDescription = "3DS Verification is returning an error."
    )

    // 1.
    val malformedDeepLinkError = PayPalSDKError(
        code = CardErrorCode.MALFORMED_DEEPLINK_URL.ordinal,
        errorDescription = "Malformed deeplink URL."
    )

    // 2. An unknown error occurred.
    val unknownError = PayPalSDKError(
        code = CardErrorCode.UNKNOWN.ordinal,
        errorDescription = "An unknown error occurred. Contact developer.paypal.com/support."
    )

    // 3. An unknown error occurred.
    fun browserSwitchError(cause: Exception) = PayPalSDKError(
        code = CardErrorCode.BROWSER_SWITCH.ordinal,
        errorDescription = cause.message ?: "Unable to Browser Switch"
    )
}
