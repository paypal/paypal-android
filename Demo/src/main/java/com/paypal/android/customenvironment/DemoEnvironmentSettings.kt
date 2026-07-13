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

    /**
     * Merchant server base URL override. Required when [selectedEnvironment] is CUSTOM.
     */
    val customMerchantBaseUrl: String = "",

    /** Venmo environment override (e.g., "sandbox", "live"). Optional, only used when testing Venmo. */
    val customVenmoEnvironment: String = "",

    /** Venmo checkout URL prefix (e.g., "qa", "staging"). Formatted as https://account.$prefix.venmo.com/go/web/paypal*/
    val customVenmoCheckoutUrlPrefix: String = "",

    /** Merchant ID override. Optional, only used when testing with CUSTOM environment. */
    val customMerchantId: String = "",
) {
    /**
     * True when the config is ready to use. Only false when [selectedEnvironment] is CUSTOM
     * and either URL field is blank.
     */
    val isValidEnvironment: Boolean
        get() = selectedEnvironment != SelectedEnvironment.CUSTOM || isValidCustomEnvironment

    private val isValidCustomEnvironment: Boolean
        get() = customSdkRestUrl.isNotBlank() &&
                customSdkGraphQLUrl.isNotBlank() &&
                customMerchantBaseUrl.isNotBlank()
}
