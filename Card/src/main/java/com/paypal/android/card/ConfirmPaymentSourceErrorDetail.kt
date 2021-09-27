package com.paypal.android.card

data class ConfirmPaymentSourceErrorDetail(
    val field: String?,
    val location: String?,
    val issue: String?,
    val description: String?
)
