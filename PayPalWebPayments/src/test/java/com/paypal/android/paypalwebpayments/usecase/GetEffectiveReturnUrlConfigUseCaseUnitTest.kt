package com.paypal.android.paypalwebpayments.usecase

import com.paypal.android.corepayments.LinkType
import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class GetEffectiveReturnUrlConfigUseCaseUnitTest {

    private val sut = GetEffectiveReturnUrlConfigUseCase()

    private val urlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "https://example.com/paypal-return",
        cancelAppUrl = "https://example.com/paypal-cancel",
        fallbackSchemeUrl = "com.example.app",
    )

    @Test
    fun `deep-link returns custom-scheme return and cancel urls with fallback preserved`() {
        val result = sut(urlConfig, LinkType.DEEP_LINK)

        val base = "com.example.app://x-callback-url/paypal-sdk/paypal-checkout"
        assertEquals(base, result.returnAppUrl)
        assertEquals("$base/cancel", result.cancelAppUrl)
        assertEquals("com.example.app", result.fallbackSchemeUrl)
    }

    @Test
    fun `app-link returns merchant https config unchanged`() {
        val result = sut(urlConfig, LinkType.APP_LINK)

        assertEquals(urlConfig, result)
    }

    @Test
    fun `returns config unchanged when fallback scheme is null even for deep-link`() {
        val configWithoutScheme = urlConfig.copy(fallbackSchemeUrl = null)

        val result = sut(configWithoutScheme, LinkType.DEEP_LINK)

        assertEquals(configWithoutScheme, result)
    }

    @Test
    fun `returns config unchanged when fallback scheme is blank even for deep-link`() {
        val configWithBlankScheme = urlConfig.copy(fallbackSchemeUrl = "  ")

        val result = sut(configWithBlankScheme, LinkType.DEEP_LINK)

        assertEquals(configWithBlankScheme, result)
    }
}
