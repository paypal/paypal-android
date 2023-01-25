package com.paypal.android.checkout
import com.paypal.android.corepayments.Environment

internal fun getPayPalEnvironment(environment: Environment) = when (environment) {
    Environment.LIVE -> com.paypal.checkout.config.Environment.LIVE
    Environment.SANDBOX -> com.paypal.checkout.config.Environment.SANDBOX
    Environment.STAGING -> com.paypal.checkout.config.Environment.STAGE
}
