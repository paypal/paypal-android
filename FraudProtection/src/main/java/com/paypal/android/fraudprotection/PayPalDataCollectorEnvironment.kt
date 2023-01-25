package com.paypal.android.fraudprotection

/**
 * Enum to set the data collector environment
 */
enum class PayPalDataCollectorEnvironment {
    LIVE,
    SANDBOX,
    STAGING
}

internal fun getMagnesEnvironment(environment: PayPalDataCollectorEnvironment) = when (environment) {
    PayPalDataCollectorEnvironment.LIVE -> lib.android.paypal.com.magnessdk.Environment.LIVE
    PayPalDataCollectorEnvironment.STAGING -> lib.android.paypal.com.magnessdk.Environment.STAGE
    PayPalDataCollectorEnvironment.SANDBOX -> lib.android.paypal.com.magnessdk.Environment.SANDBOX
}
