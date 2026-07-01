package com.paypal.android.corepayments

enum class Environment(internal open val url: String, open val graphQLEndpoint: String) {
    LIVE(
        "https://api-m.paypal.com",
        "https://www.paypal.com"
    ),
    SANDBOX(
        "https://api-m.sandbox.paypal.com",
        "https://www.sandbox.paypal.com"
    ),
    CUSTOM("", "") {
        override val url get() = customRestUrl
        override val graphQLEndpoint get() = customGraphQLUrl
    };

    companion object {
        var customRestUrl: String = ""
        var customGraphQLUrl: String = ""
    }
}
