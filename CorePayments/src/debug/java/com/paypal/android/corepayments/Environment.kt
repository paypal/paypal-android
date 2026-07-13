package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

enum class Environment {
    LIVE, SANDBOX, CUSTOM;

    internal val url: String
        get() = when (this) {
            LIVE -> "https://api-m.paypal.com"
            SANDBOX -> "https://api-m.sandbox.paypal.com"
            CUSTOM -> customRestUrl
        }

    internal val graphQLEndpoint: String
        get() = when (this) {
            LIVE -> "https://www.paypal.com"
            SANDBOX -> "https://www.sandbox.paypal.com"
            CUSTOM -> customGraphQLUrl
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoEnvironment: String
        get() = when (this) {
            LIVE -> "live"
            SANDBOX -> "sandbox"
            CUSTOM -> customVenmoEnvironment
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoCheckoutBaseUrl: String
        get() = when (this) {
            LIVE, SANDBOX -> "https://account.venmo.com/go/web/paypal"
            CUSTOM -> customVenmoCheckoutBaseUrl
        }

    companion object {
        var customRestUrl: String = ""
        var customGraphQLUrl: String = ""
        var customVenmoEnvironment: String = ""
        var customVenmoCheckoutBaseUrl: String = ""
    }
}
