package com.paypal.android.core

data class CoreConfig(
    val clientId: String,
    val clientSecret: String,
    val environment: Environment = Environment.SANDBOX
)
