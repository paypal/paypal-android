package com.paypal.android.core

data class CoreConfig @JvmOverloads constructor(
    val clientId: String = "",
    val accessToken: String = "",
    val environment: Environment = Environment.SANDBOX,
)
