package com.paypal.android.fraudprotection

/**
 * Enum to set the data collector environment
 */
enum class PayPalDataCollectorEnvironment {
    LIVE,
    SANDBOX
}

internal fun getMagnesEnvironment(environment: PayPalDataCollectorEnvironment) = when (environment) {
    PayPalDataCollectorEnvironment.LIVE -> lib.android.paypal.com.magnessdk.Environment.LIVE
    PayPalDataCollectorEnvironment.SANDBOX -> lib.android.paypal.com.magnessdk.Environment.SANDBOX
}
