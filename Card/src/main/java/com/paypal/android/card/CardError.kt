package com.paypal.android.card

data class CardError(val name: String?, val details: List<ErrorDetail>, val message: String?)

data class ErrorDetail(val field: String?, val location: String?, val issue: String?, val description: String?)

