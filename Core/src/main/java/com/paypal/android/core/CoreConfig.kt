package com.paypal.android.core

/**
 * Configuration for SDK requests.
 *
 * @property clientId your client ID from the PayPal Developer Portal
 * @property clientSecret
 * @property environment the [Environment] for the payment requests
 */
data class CoreConfig(
    val clientId: String,
    // TODO: Spike to investigate if this can be removed yet
    val clientSecret: String = "",
    val environment: Environment = Environment.SANDBOX,
)
