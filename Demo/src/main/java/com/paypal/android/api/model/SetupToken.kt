package com.paypal.android.api.model

data class SetupToken(
    val id: String,
    val customerId: String,
    val status: String
)
