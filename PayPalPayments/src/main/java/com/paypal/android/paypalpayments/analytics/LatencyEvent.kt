@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalpayments.analytics

internal enum class LatencyEvent(val value: String) {
    API_REQUEST_LATENCY("paypal-payments:api-request-latency"),
    USER_PERCEIVED_LATENCY("paypal-payments:user-perceived-latency"),
}

internal object PresentationType {
    const val APP_SWITCH = "app-switch"
    const val BROWSER = "browser"
    const val ERROR = "error"
}

internal object LatencyFlow {
    const val CHECKOUT = "checkout"
    const val VAULT = "vault"
}

internal object LatencyEndpoint {
    const val UPDATE_CLIENT_CONFIG = "/graphql/UpdateClientConfig"
    const val CREATE_ORDER = "/v2/checkout/orders"
    const val CREATE_SESSION = "/graphql/createShopperSessionWithAppSwitchEligibility"
}
