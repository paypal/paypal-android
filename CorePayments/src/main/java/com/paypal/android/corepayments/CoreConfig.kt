package com.paypal.android.corepayments

data class CoreConfig @JvmOverloads constructor(
    val accessToken: String? = null,
    val environment: Environment = Environment.SANDBOX,
)
