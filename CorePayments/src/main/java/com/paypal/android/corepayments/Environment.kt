package com.paypal.android.corepayments

enum class Environment(val url: String, val graphQLEndpoint: String) {
    LIVE(
        "https://api.paypal.com",
        "https://www.paypal.com"
    ),
    SANDBOX(
        "https://api.sandbox.paypal.com",
        "https://www.sandbox.paypal.com"
    ),
}
