package com.paypal.android.card

data class CardResult(val response: CardResponse? = null, val error: CardError? = null)

data class CardResponse(
    val status: String?,
    val lastDigits: String?,
    val brand: String?,
    val type: String?
)