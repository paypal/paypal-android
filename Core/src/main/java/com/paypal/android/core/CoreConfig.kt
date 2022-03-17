package com.paypal.android.core

/**
 * Configuration for SDK requests.
 *
 * @property clientId your client ID from the PayPal Developer Portal
 * @property clientSecret
 * @property environment the [Environment] for the payment requests
 */
// TODO - Spike to investigate if clientSecret is still needed
data class CoreConfig @JvmOverloads constructor(
    val clientId: String,
    val clientSecret: String = "",
    val environment: Environment = Environment.SANDBOX,
)
