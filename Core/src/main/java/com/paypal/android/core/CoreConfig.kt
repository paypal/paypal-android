package com.paypal.android.core

data class CoreConfig(
    val clientId: String,
    val environment: Environment = Environment.SANDBOX,
    val clientSecret: String = "",
)
