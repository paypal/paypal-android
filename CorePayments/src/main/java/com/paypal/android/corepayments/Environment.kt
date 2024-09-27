package com.paypal.android.corepayments

import kotlinx.serialization.Serializable

@Serializable
enum class Environment(internal val url: String, internal val graphQLEndpoint: String) {
    LIVE(
        "https://api-m.paypal.com",
        "https://www.paypal.com"
    ),
    SANDBOX(
        "https://api-m.sandbox.paypal.com",
        "https://www.sandbox.paypal.com"
    ),
}
