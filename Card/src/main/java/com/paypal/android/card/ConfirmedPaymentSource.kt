package com.paypal.android.card

data class ConfirmedPaymentSource(
    val orderID: String?,
    val status: String?,
    val lastDigits: String?,
    val brand: String?,
    val type: String?
)
