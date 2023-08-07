package com.paypal.android.api.model

data class PaymentToken(
    val id: String,
    val customerId: String,
    val cardLast4: String,
    val cardBrand: String
)
