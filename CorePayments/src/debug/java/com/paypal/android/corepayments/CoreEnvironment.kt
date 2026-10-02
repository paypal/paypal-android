package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

enum class CoreEnvironment(internal open val url: String, internal open val graphQLEndpoint: String) {
    // TODO: Look into improving the quality of this.
    //  CUSTOM is supposed to be static, but values are dynamic.
    //  url and graphQLEndpoint can be separated out, so that we don't have them dependent on the enum.
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

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoCheckoutBaseUrl: String
        get() = when (this) {
            LIVE -> "https://account.venmo.com/go/web/paypal"
            SANDBOX -> "https://account.ext.live.venmo.com/go/web/paypal" // ToDo: update new url after fixed in sandbox
            CUSTOM -> customVenmoCheckoutBaseUrl
        }

    companion object {
        var customRestUrl: String = ""
        var customGraphQLUrl: String = ""
        var customVenmoCheckoutBaseUrl: String = ""
    }
}
