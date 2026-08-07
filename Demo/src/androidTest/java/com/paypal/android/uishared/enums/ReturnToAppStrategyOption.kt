package com.paypal.android.uishared.enums

/**
 * Test-only strategy options used by espresso tests to drive the "RETURN TO APP STRATEGY"
 * setting in the Demo app UI. The SDK now auto-selects between App Links and a custom URL
 * scheme at runtime, so this enum only exists to parameterize test scenarios.
 */
enum class ReturnToAppStrategyOption {
    APP_LINKS,
    CUSTOM_URL_SCHEME
}
