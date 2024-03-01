package com.paypal.android.cardpayments

internal data class UpdateSetupTokenResult(
    val setupTokenId: String,
    val status: String,
    val approveHref: String?
)
