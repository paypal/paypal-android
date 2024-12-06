package com.paypal.android.cardpayments

data class SetupTokenInfo(
    val setupTokenId: String,
    val status: String? = null,
    val didAttemptThreeDSecureAuthentication: Boolean = false
)
