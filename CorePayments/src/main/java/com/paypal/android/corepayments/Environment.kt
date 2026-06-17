package com.paypal.android.corepayments

/**
 * Represents the PayPal environment to use for API calls.
 *
 * Use [LIVE] or [SANDBOX] for standard environments.
 * Use [Custom] to point the SDK at a custom backend (e.g. a stage or QA environment)
 * without hard-coding those URLs in a public repository.
 */
sealed class Environment(
    internal val url: String,
    val graphQLEndpoint: String,
    val name: String,
) {
    /** PayPal production environment. */
    object LIVE : Environment(
        url = "https://api-m.paypal.com",
        graphQLEndpoint = "https://www.paypal.com",
        name = "live"
    )

    /** PayPal sandbox environment. */
    object SANDBOX : Environment(
        url = "https://api-m.sandbox.paypal.com",
        graphQLEndpoint = "https://www.sandbox.paypal.com",
        name = "sandbox"
    )

    /**
     * A fully custom environment.
     *
     * @param customUrl Base URL for native REST API calls (e.g. "https://api.msmaster.qa.paypal.com").
     * @param customGraphQLEndpoint Base URL for GraphQL calls — "/graphql" is appended automatically
     *   (e.g. "https://www.braintree.stage.paypal.com").
     */
    data class Custom(
        val customUrl: String,
        val customGraphQLEndpoint: String
    ) : Environment(
        url = customUrl.trim().trimEnd('/'),
        graphQLEndpoint = customGraphQLEndpoint.trim().trimEnd('/'),
        name = "sandbox"
    )
}
