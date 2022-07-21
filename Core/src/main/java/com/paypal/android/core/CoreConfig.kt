package com.paypal.android.core

data class CoreConfig @JvmOverloads constructor(
    val accessToken: String? = null,
    val environment: Environment = Environment.SANDBOX,
)
