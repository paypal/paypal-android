package com.paypal.android.corepayments

data class CoreConfig @JvmOverloads constructor(
    val clientId: String,
    val merchantId: String,
    val coreEnvironment: CoreEnvironment = CoreEnvironment.SANDBOX,
    val bnCode: String? = null,
)
