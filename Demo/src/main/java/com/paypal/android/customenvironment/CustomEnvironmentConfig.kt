package com.paypal.android.customenvironment

/**
 * Holds the SDK URLs for a custom environment.
 *
 * All values are blank by default; [isConfigured] is true only when both URL fields are filled in.
 */
data class CustomEnvironmentConfig(
    /** Base URL for PayPal SDK native REST calls. */
    val sdkRestUrl: String = "",

    /** Base URL for PayPal SDK GraphQL calls ("/graphql" is appended automatically). */
    val sdkGraphQLUrl: String = "",

    /** Optional client ID override. When blank, falls back to the default integration client ID. */
    val clientId: String = "",
) {
    /** True only when both URL fields are non-blank. */
    val isConfigured: Boolean
        get() = sdkRestUrl.isNotBlank() && sdkGraphQLUrl.isNotBlank()
}
