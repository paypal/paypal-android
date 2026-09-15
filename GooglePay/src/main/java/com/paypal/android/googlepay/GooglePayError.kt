package com.paypal.android.googlepay

import com.google.android.gms.common.api.Status
import com.paypal.android.corepayments.PayPalSDKError

internal object GooglePayError {

    val missingPaymentData = PayPalSDKError(
        code = GooglePayErrorCode.MISSING_PAYMENT_DATA.ordinal,
        errorDescription = "The Google Pay response is missing payment data required to finish payment method authorization."
    )

    fun developerError(status: Status): PayPalSDKError {
        val statusCode = status.statusCode
        val statusMessage = status.statusMessage
        return PayPalSDKError(
            code = GooglePayErrorCode.DEVELOPER_ERROR.ordinal,
            errorDescription = "Google Pay Developer Error with Code: $statusCode – $statusMessage"
        )
    }

    fun unknownError(status: Status): PayPalSDKError {
        val statusCode = status.statusCode
        val statusMessage = status.statusMessage
        return PayPalSDKError(
            code = GooglePayErrorCode.DEVELOPER_ERROR.ordinal,
            errorDescription = "Unknown Google Pay Error with Code: $statusCode – $statusMessage"
        )
    }
}