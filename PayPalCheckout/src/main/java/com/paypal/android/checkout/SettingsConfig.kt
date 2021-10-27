package com.paypal.android.checkout

/**
 * [SettingsConfig] holds additional config values that are used for debugging and testing.
 */
class SettingsConfig(

    /**
     * Enables Checkout SDK logs. It is recommended to disable this when releasing to production.
     */
    val loggingEnabled: Boolean = false,

    /**
     * This should remain false in production!
     *
     * Enabling this will cause eligibility to always fail. This provides a way to test web
     * fallback.
     */
    val shouldFailEligibility: Boolean = false
) {
    internal val asNativeCheckout: com.paypal.checkout.config.SettingsConfig
        get() = com.paypal.checkout.config.SettingsConfig(loggingEnabled, shouldFailEligibility)
}
