package com.paypal.android.corepayments

enum class Environment(val url: String, val grqphQlUrl: String) {
    LIVE(
        "https://api.paypal.com",
        "https://www.paypal.com/graphql"
    ),
    SANDBOX(
        "https://api.sandbox.paypal.com",
        "https://www.sandbox.paypal.com/graphql"
    ),
    STAGING(
        "https://api.msmaster.qa.paypal.com",
        "https:///www.msmaster.qa.paypal.com/graphql"
    )
}
