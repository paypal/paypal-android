package com.paypal.android.corepayments

data class CoreConfig @JvmOverloads constructor(
    val clientId: String,
    val environment: Environment = Environment.SANDBOX,
    val merchantID: String = "",
    val bnCode: String? = null,
)
