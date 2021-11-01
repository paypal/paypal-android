package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.core.Environment
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PayPalConfigurationTest {

    private val applicationMock = mockk<Application>(relaxed = true)
    private val clientIdMock = generateRandomString()
    private val returnUrlMock = "com.example://paypalpay"
    private val currencyCodeMock = mockk<CurrencyCode>(relaxed = true)
    private val userActionMock = mockk<UserAction>(relaxed = true)
    private val settingsConfigMock = mockk<SettingsConfig>(relaxed = true)
    private val environmentMock = mockk<Environment>(relaxed = true)

    @Test
    fun `payPalConfiguration initialized with correct values`() {
        val payPalConfiguration = PayPalConfiguration(
            application = applicationMock,
            clientId = clientIdMock,
            environment = environmentMock,
            returnUrl = returnUrlMock,
            currencyCode = currencyCodeMock,
            userAction = userActionMock,
            settingsConfig = settingsConfigMock
        )

        expectThat(payPalConfiguration) {
            get { application }.isEqualTo(applicationMock)
            get { paymentsConfiguration.clientId }.isEqualTo(clientIdMock)
            get { paymentsConfiguration.environment }.isEqualTo(environmentMock)
            get { returnUrl }.isEqualTo(returnUrlMock)
            get { currencyCode }.isEqualTo(currencyCodeMock)
            get { userAction }.isEqualTo(userActionMock)
            get { settingsConfig }.isEqualTo(settingsConfigMock)
        }
    }
}
