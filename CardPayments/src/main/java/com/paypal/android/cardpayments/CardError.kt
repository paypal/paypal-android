package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

internal object CardError {

    // 0. An error from 3DS verification
    val threeDSVerificationError = PayPalSDKError(
        code = CardErrorCode.THREEDS_VERIFICATION_FAILED.ordinal,
        errorDescription = "3DS Verification is returning an error."
    )
}
