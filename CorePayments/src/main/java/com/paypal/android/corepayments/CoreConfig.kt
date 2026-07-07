package com.paypal.android.corepayments

data class CoreConfig @JvmOverloads constructor(
    val clientId: String,
    val merchantId: String,
    val environment: Environment = Environment.SANDBOX,
    val bnCode: String? = null,
) {

    @Deprecated(
        message = "Provide merchantId. Use CoreConfig(clientId, merchantId) instead.",
        level = DeprecationLevel.WARNING
    )
    constructor(clientId: String) : this(clientId, "")

    @Deprecated(
        message = "Provide merchantId. Use CoreConfig(clientId, merchantId, environment) instead.",
        level = DeprecationLevel.WARNING
    )
    constructor(clientId: String, environment: Environment) : this(clientId, "", environment)
}
