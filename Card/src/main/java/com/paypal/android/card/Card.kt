package com.paypal.android.card

data class Card(
    val number: String,
    val expirationDate: String,
    val securityCode: String
)
