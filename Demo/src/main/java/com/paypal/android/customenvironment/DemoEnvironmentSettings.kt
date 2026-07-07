package com.paypal.android.customenvironment

/**
 * Holds all demo app environment settings: which environment is selected and
 * the optional custom SDK URLs used when [selectedEnvironment] is [SelectedEnvironment.CUSTOM].
 */
data class DemoEnvironmentSettings(
    val selectedEnvironment: SelectedEnvironment = SelectedEnvironment.SANDBOX,

    /** Base URL for PayPal SDK native REST calls. Only used when [selectedEnvironment] is CUSTOM. */
    val customSdkRestUrl: String = "",

    /**
     * Base URL for PayPal SDK GraphQL calls. Only used when [selectedEnvironment] is CUSTOM.
     */
    val customSdkGraphQLUrl: String = "",

    /** Optional client ID override. When blank, falls back to the default integration client ID. */
    val customClientId: String = "",
) {
    /**
     * True when the config is ready to use:
     * - Always true for LIVE and SANDBOX.
     * - True for CUSTOM only when both URL fields are non-blank.
     */
    val isConfigured: Boolean
        get() = when (selectedEnvironment) {
            SelectedEnvironment.LIVE, SelectedEnvironment.SANDBOX -> true
            SelectedEnvironment.CUSTOM -> customSdkRestUrl.isNotBlank() && customSdkGraphQLUrl.isNotBlank()
        }
}
