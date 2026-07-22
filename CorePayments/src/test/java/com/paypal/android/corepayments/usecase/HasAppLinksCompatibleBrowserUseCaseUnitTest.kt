package com.paypal.android.corepayments.usecase

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HasAppLinksCompatibleBrowserUseCaseUnitTest {

    private val getDefaultAppUseCase: GetDefaultAppUseCase = mockk()
    private val uri: Uri = Uri.parse("https://www.paypal.com/checkout")
    private lateinit var sut: HasAppLinksCompatibleBrowserUseCase

    @Before
    fun beforeEach() {
        sut = HasAppLinksCompatibleBrowserUseCase(getDefaultAppUseCase)
    }

    @Test
    fun `returns true for each App-Links-compatible browser`() {
        val compatibleBrowsers = listOf(
            "com.android.chrome",
            "com.android.chrome.canary", // substring match
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
        )

        compatibleBrowsers.forEach { browser ->
            every { getDefaultAppUseCase(uri) } returns browser
            assertTrue("expected $browser to be compatible", sut(uri))
        }
    }

    @Test
    fun `returns false for browsers not on the allowlist`() {
        val incompatible = listOf(
            "com.opera.browser",
            "com.duckduckgo.mobile.android",
            "com.yandex.browser",
            "com.mi.globalbrowser",
            "com.some.unknown.app",
        )

        incompatible.forEach { browser ->
            every { getDefaultAppUseCase(uri) } returns browser
            assertFalse("expected $browser to be incompatible", sut(uri))
        }
    }

    @Test
    fun `returns false when no default handler resolves`() {
        every { getDefaultAppUseCase(uri) } returns null

        assertFalse(sut(uri))
    }
}
