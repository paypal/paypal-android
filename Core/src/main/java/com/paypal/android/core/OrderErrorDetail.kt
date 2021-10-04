package com.paypal.android.core

data class OrderErrorDetail(
    val field: String?,
    val location: String?,
    val issue: String?,
    val description: String?
)
