package com.paypal.android.paypalwebpayments.usecase

import com.paypal.android.corepayments.LinkType
import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetEffectiveReturnUrlConfigUseCaseUnitTest {

    private val sut = GetEffectiveReturnUrlConfigUseCase()

    private val urlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "https://example.com/paypal-return",
        cancelAppUrl = "https://example.com/paypal-cancel",
        fallbackSchemeUrl = "com.example.app",
    )

    @Test
    fun `deep-link returns distinct success and cancel custom-scheme urls, with fallback preserved`() {
        val result = sut(urlConfig, LinkType.DEEP_LINK)

        val base = "com.example.app://x-callback-url/paypal-sdk/paypal-checkout"
        assertEquals("$base/success", result.returnAppUrl)
        assertEquals("$base/cancel", result.cancelAppUrl)
        assertEquals("com.example.app", result.fallbackSchemeUrl)
    }

    @Test
    fun `deep-link preserves merchant query params from the original return and cancel urls`() {
        val configWithQueryParams = urlConfig.copy(
            returnAppUrl = "https://example.com/paypal-return?ref=123",
            cancelAppUrl = "https://example.com/paypal-cancel?ref=456",
        )

        val result = sut(configWithQueryParams, LinkType.DEEP_LINK)

        val base = "com.example.app://x-callback-url/paypal-sdk/paypal-checkout"
        assertEquals("$base/success?ref=123", result.returnAppUrl)
        assertEquals("$base/cancel?ref=456", result.cancelAppUrl)
    }

    @Test
    fun `app-link returns merchant https config unchanged`() {
        val result = sut(urlConfig, LinkType.APP_LINK)

        assertEquals(urlConfig, result)
    }

    @Test
    fun `returns config unchanged when fallback scheme is blank even for deep-link`() {
        val configWithBlankScheme = urlConfig.copy(fallbackSchemeUrl = "  ")

        val result = sut(configWithBlankScheme, LinkType.DEEP_LINK)

        assertEquals(configWithBlankScheme, result)
    }
}
