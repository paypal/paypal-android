package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

enum class Environment {
    LIVE, SANDBOX;

    internal val url: String
        get() = when (this) {
            LIVE -> "https://api-m.paypal.com"
            SANDBOX -> "https://api-m.sandbox.paypal.com"
        }

    internal val graphQLEndpoint: String
        get() = when (this) {
            LIVE -> "https://www.paypal.com"
            SANDBOX -> "https://www.sandbox.paypal.com"
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoEnvironment: String
        get() = when (this) {
            LIVE -> "live"
            SANDBOX -> "sandbox"
        }
}
