package com.paypal.android

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Credentials are loaded from instrumentation test arguments which are populated from
 * environment variables in build.gradle:
 * - SANDBOX_ACCOUNT_USERNAME: PayPal sandbox account email
 * - SANDBOX_ACCOUNT_PASSWORD: PayPal sandbox account password
 */
object TestConfig {
    private const val SANDBOX_ACCOUNT_USERNAME = "SANDBOX_ACCOUNT_USERNAME"
    private const val SANDBOX_ACCOUNT_PASSWORD = "SANDBOX_ACCOUNT_PASSWORD"

    private val arguments = InstrumentationRegistry.getArguments()

    val TEST_EMAIL: String = arguments.getString(SANDBOX_ACCOUNT_USERNAME)
        ?: throw error("Missing $SANDBOX_ACCOUNT_USERNAME in environment variables")

    val TEST_PASSWORD: String = arguments.getString(SANDBOX_ACCOUNT_PASSWORD)
        ?: throw error("Missing $SANDBOX_ACCOUNT_PASSWORD in environment variables")
}
