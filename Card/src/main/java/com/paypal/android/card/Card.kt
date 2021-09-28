package com.paypal.android.card

data class Card(
    var number: String = "",
    var expirationDate: String = "",
    var securityCode: String = ""
)
