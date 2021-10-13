package com.paypal.android.core

data class OrderError(
    val name: String,
    val message: String,
    val details: List<OrderErrorDetail> = emptyList()
)
