package com.paypal.android.core

data class OrderError(
    val name: String? = null,
    val details: List<OrderErrorDetail>? = null,
    val message: String? = null
)
