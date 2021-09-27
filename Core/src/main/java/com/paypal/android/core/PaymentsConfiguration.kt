package com.paypal.android.core

data class PaymentsConfiguration(
    val clientId: String,
    val clientSecret: String,
    val environment: Environment = Environment.SANDBOX
)
