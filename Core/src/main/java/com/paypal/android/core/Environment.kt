package com.paypal.android.core

enum class Environment(val url: String, val grqphQlUrl: String) {
    LIVE(
        "https://api.paypal.com",
        "https://paypal.com/graphql"
    ),
    SANDBOX(
        "https://api.sandbox.paypal.com",
        "https://sandbox.paypal.com/graphql"
    ),
    STAGING(
        "https://api.msmaster.qa.paypal.com",
        "https://msmaster.qa.paypal.com/graphql"
    )
}
