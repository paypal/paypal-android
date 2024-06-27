package com.paypal.android.api.model

data class PayPalSetupToken(
    val id: String,
    val customerId: String,
    val status: String
)
