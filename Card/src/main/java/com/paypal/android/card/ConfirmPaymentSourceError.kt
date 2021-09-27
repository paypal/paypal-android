package com.paypal.android.card

data class ConfirmPaymentSourceError(val name: String? = null, val details: List<ConfirmPaymentSourceErrorDetail>? = null, val message: String? = null)
