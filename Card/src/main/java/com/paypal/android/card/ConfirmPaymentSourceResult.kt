package com.paypal.android.card

data class ConfirmPaymentSourceResult(
    val response: ConfirmedPaymentSource? = null,
    val error: ConfirmPaymentSourceError? = null
)
