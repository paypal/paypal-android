package com.paypal.android.api.model

data class CardPaymentToken(
    val id: String,
    val customerId: String,
    val cardLast4: String,
    val cardBrand: String
)
