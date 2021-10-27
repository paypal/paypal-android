package com.paypal.android.checkout

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

class SettingsConfigTest {

    @Test
    fun `settingsConfig with correct values and transformed to native checkout`() {
        val settingsConfig = SettingsConfig(true, true)
        expectThat(settingsConfig) {
            get { loggingEnabled }.isTrue()
            get { shouldFailEligibility }.isTrue()
        }
        expectThat(settingsConfig.asNativeCheckout) {
            get { loggingEnabled }.isTrue()
            get { shouldFailEligibility }.isTrue()
        }
    }
}
