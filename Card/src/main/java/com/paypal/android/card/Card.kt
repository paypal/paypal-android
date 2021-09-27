package com.paypal.android.card

data class Card(
    val number: String,
    var expirationDate: String,
    val securityCode: String
)
