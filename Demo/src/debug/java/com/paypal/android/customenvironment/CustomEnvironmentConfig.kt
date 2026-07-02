package com.paypal.android.customenvironment

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment

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

    /**
     * Builds a [CoreConfig] backed by [Environment.CUSTOM], or returns [fallbackConfig]
     * when the custom environment is not fully configured.
     */
    fun toCoreConfig(fallbackConfig: CoreConfig): CoreConfig =
        if (isConfigured) {
            Environment.customRestUrl = sdkRestUrl.trim().trimEnd('/')
            Environment.customGraphQLUrl = sdkGraphQLUrl.trim().trimEnd('/')
            val resolvedClientId = clientId.trim().ifBlank { fallbackConfig.clientId }
            CoreConfig(clientId = resolvedClientId, environment = Environment.CUSTOM)
        } else {
            fallbackConfig
        }
}
