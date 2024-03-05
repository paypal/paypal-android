package com.paypal.android.api.model

data class CardSetupToken(
    val id: String,
    val customerId: String,
    val status: String,
    val verificationStatus: String? = null,
)
