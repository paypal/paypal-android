package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

enum class CoreEnvironment(internal val url: String, internal val graphQLEndpoint: String) {
    LIVE(
        "https://api-m.paypal.com",
        "https://www.paypal.com"
    ),
    SANDBOX(
        "https://api-m.sandbox.paypal.com",
        "https://www.sandbox.paypal.com"
    ),
    ;

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val venmoCheckoutBaseUrl: String
        get() = when (this) {
            LIVE -> "https://account.venmo.com/go/web/paypal"
            SANDBOX -> "https://account.ext.live.venmo.com/go/web/paypal" // ToDo: update new url after fixed in sandbox
        }
}
