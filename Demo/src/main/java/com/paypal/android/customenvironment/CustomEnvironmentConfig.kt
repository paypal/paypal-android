package com.paypal.android.customenvironment

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment

/**
 * Holds the URLs and credentials for a custom (e.g. stage) environment.
 * All fields are blank by default; [isConfigured] is true only when every field is filled in.
 */
data class CustomEnvironmentConfig(
    /** PayPal client ID for the custom environment. */
    val clientId: String = "",

    /**
     * Base URL for PayPal SDK native REST calls.
     * Example: "https://api.msmaster.qa.paypal.com"
     */
    val sdkRestUrl: String = "",

    /**
     * Base URL for PayPal SDK GraphQL calls ("/graphql" is appended automatically).
     * Example: "https://www.braintree.stage.paypal.com"
     */
    val sdkGraphQLUrl: String = "",

    /**
     * Base URL for the demo-app merchant server (order creation, capture, etc.).
     * Example: "https://www.braintree.stage.paypal.com/mockmerchantnodeweb/"
     */
    val merchantServerUrl: String = "",

    /**
     * Override for the create-order and capture/authorize paths.
     *
     * Leave blank to use the default `/orders` path.
     * For the XOSphere stage mock merchant server, enter the full base path:
     *   `/PPCP/stage_modxo/v2/checkout/orders`
     *
     * Capture and authorize paths are derived automatically by appending
     * `/{orderId}/capture` and `/{orderId}/authorize` to this value.
     */
    val createOrderPath: String = "",

    /**
     * Override for the create-setup-token and get-setup-token paths.
     *
     * Leave blank to use the default `/setup-tokens` path.
     * For the XOSphere stage mock merchant server:
     *   `/PPCP/stage_modxo/v3/vault/setup-tokens`
     *
     * Get-setup-token is derived automatically by appending `/{setupTokenId}` to this value.
     */
    val createSetupTokenPath: String = "",

    /**
     * Override for the create-payment-token path.
     *
     * Leave blank to use the default `/payment-tokens` path.
     * For the XOSphere stage mock merchant server:
     *   `/PPCP/stage_modxo/v3/vault/payment-tokens`
     */
    val createPaymentTokenPath: String = ""
) {
    /** True only when all four URL/credential fields are non-blank. */
    val isConfigured: Boolean
        get() = clientId.isNotBlank()
            && sdkRestUrl.isNotBlank()
            && sdkGraphQLUrl.isNotBlank()
            && merchantServerUrl.isNotBlank()

    /** True when at least one custom path override has been entered. */
    val hasCustomPaths: Boolean
        get() = createOrderPath.isNotBlank()
            || createSetupTokenPath.isNotBlank()
            || createPaymentTokenPath.isNotBlank()

    /** Builds a [CoreConfig] backed by [Environment.Custom], or falls back to [fallbackConfig]. */
    fun toCoreConfig(fallbackConfig: CoreConfig): CoreConfig =
        if (isConfigured) {
            CoreConfig(
                clientId = clientId,
                environment = Environment.Custom(
                    customUrl = sdkRestUrl,
                    customGraphQLEndpoint = sdkGraphQLUrl
                )
            )
        } else {
            fallbackConfig
        }
}
