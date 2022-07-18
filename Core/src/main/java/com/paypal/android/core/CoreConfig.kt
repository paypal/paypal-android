package com.paypal.android.core

data class CoreConfig @JvmOverloads constructor(
    val clientId: String? = null,
    val accessToken: String? = null,
    val environment: Environment = Environment.SANDBOX,
)
