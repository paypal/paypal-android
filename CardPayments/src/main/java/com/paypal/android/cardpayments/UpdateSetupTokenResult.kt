package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

internal sealed class UpdateSetupTokenResult {

    data class Success(
        val setupTokenId: String,
        val status: String,
        val approveHref: String?
    ) : UpdateSetupTokenResult()

    data class Failure(val error: PayPalSDKError) : UpdateSetupTokenResult()
}
