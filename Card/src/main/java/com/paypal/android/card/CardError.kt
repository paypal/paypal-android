package com.paypal.android.card

data class CardError(val name: String? = null, val details: List<ErrorDetail>? = null, val message: String? = null)

data class ErrorDetail(val field: String?, val location: String?, val issue: String?, val description: String?)
