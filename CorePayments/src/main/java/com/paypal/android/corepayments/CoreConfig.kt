package com.paypal.android.corepayments

data class CoreConfig @JvmOverloads constructor(
    val accessToken: String,
    val environment: Environment = Environment.SANDBOX,
)
