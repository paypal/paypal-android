package com.paypal.android.card

data class Card(
    val cardNumber: String,
    val expirationDate: String,
    val securityCode: String
)
