package com.paypal.android.corepayments.usecase

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.common.DeviceInspector
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("MaxLineLength")
@RunWith(RobolectricTestRunner::class)
class GetReturnToAppStrategyUseCaseUnitTest {

    private val applicationContext: Context = mockk()
    private val packageManager: PackageManager = mockk()
    private val deviceInspector: DeviceInspector = mockk()

    private val merchantPackageName = "com.merchant.app"
    private val appLinkReturnUrl = "https://merchant.com/return"
    private val appLinkReturnUri: Uri = Uri.parse(appLinkReturnUrl)
    private val checkoutUri: Uri = Uri.parse("https://example.com/checkout")
    private val fallbackScheme = "com.merchant.app.paypal"

    private lateinit var sut: GetReturnToAppStrategyUseCase

    @Before
    fun beforeEach() {
        every { applicationContext.packageName } returns merchantPackageName
        every { applicationContext.packageManager } returns packageManager
        every { deviceInspector.canResolvePayPalAppSwitch() } returns false
        stubDefaultApp(checkoutUri, packageName = null)
        sut = GetReturnToAppStrategyUseCase(applicationContext, deviceInspector)
    }

    /** Stubs the resolved default handler's package name for [uri], or `null` if none resolves. */
    private fun stubDefaultApp(uri: Uri, packageName: String?) {
        val resolveInfo = packageName?.let {
            ResolveInfo().apply { activityInfo = ActivityInfo().apply { this.packageName = it } }
        }
        every {
            packageManager.resolveActivity(match { it.data == uri }, PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo
    }

    @Test
    fun `AppLink when merchant is default handler and an App-Links-compatible browser is available`() {
        stubDefaultApp(appLinkReturnUri, merchantPackageName)
        stubDefaultApp(checkoutUri, "com.android.chrome")

        val expected = GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(appLinkReturnUrl))
        assertEquals(expected, sut(appLinkReturnUrl, fallbackScheme, checkoutUri))
    }

    @Test
    fun `AppLink when default handler is each App-Links-compatible browser`() {
        val compatibleBrowsers = listOf(
            "com.android.chrome",
            "com.android.chrome.canary", // substring match
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
        )

        compatibleBrowsers.forEach { browser ->
            stubDefaultApp(appLinkReturnUri, merchantPackageName)
            stubDefaultApp(checkoutUri, browser)

            assertEquals(
                "expected $browser to be treated as App-Links-compatible",
                GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(appLinkReturnUrl)),
                sut(appLinkReturnUrl, fallbackScheme, checkoutUri)
            )
        }
    }

    @Test
    fun `CustomUrlScheme when default handler is a browser not on the allowlist`() {
        val incompatibleBrowsers = listOf(
            "com.opera.browser",
            "com.duckduckgo.mobile.android",
            "com.yandex.browser",
            "com.mi.globalbrowser",
            "com.some.unknown.app",
        )

        incompatibleBrowsers.forEach { browser ->
            stubDefaultApp(appLinkReturnUri, merchantPackageName)
            stubDefaultApp(checkoutUri, browser)

            assertEquals(
                "expected $browser to be treated as incompatible",
                GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)),
                sut(appLinkReturnUrl, fallbackScheme, checkoutUri)
            )
        }
    }

    @Test
    fun `AppLink when merchant is default handler and the PayPal app can app switch`() {
        stubDefaultApp(appLinkReturnUri, merchantPackageName)
        every { deviceInspector.canResolvePayPalAppSwitch() } returns true

        val expected = GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(appLinkReturnUrl))
        assertEquals(expected, sut(appLinkReturnUrl, fallbackScheme, checkoutUri))
    }

    @Test
    fun `CustomUrlScheme when merchant is default handler but no compatible browser or first-party app`() {
        // AC1: default links disabled at the browser level -> fall back to deep link
        stubDefaultApp(appLinkReturnUri, merchantPackageName)
        every { deviceInspector.canResolvePayPalAppSwitch() } returns false

        assertEquals(
            GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)),
            sut(appLinkReturnUrl, fallbackScheme, checkoutUri)
        )
    }

    @Test
    fun `CustomUrlScheme when merchant app is not the default handler for its own return uri`() {
        // AC1: "Open supported links" unchecked for the merchant app -> not verified -> deep link
        stubDefaultApp(appLinkReturnUri, "com.other.app")
        stubDefaultApp(checkoutUri, "com.android.chrome")

        assertEquals(
            GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)),
            sut(appLinkReturnUrl, fallbackScheme, checkoutUri)
        )
    }

    @Test
    fun `CustomUrlScheme when the app link return url is blank`() {
        stubDefaultApp(checkoutUri, "com.android.chrome")

        assertEquals(
            GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.CustomUrlScheme(fallbackScheme)),
            sut(appLinkReturnUrl = "", fallbackSchemeUrl = fallbackScheme, checkoutUri = checkoutUri)
        )
    }

    @Test
    fun `AppLink when App Links won't route but no fallback scheme is available`() {
        // Deep-link conditions, but there is no custom scheme to switch to -> App Link.
        stubDefaultApp(appLinkReturnUri, "com.other.app")

        assertEquals(
            GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(appLinkReturnUrl)),
            sut(appLinkReturnUrl, fallbackSchemeUrl = "", checkoutUri = checkoutUri)
        )
    }

    @Test
    fun `AppLink when App Links won't route but fallback scheme is blank`() {
        stubDefaultApp(appLinkReturnUri, "com.other.app")

        assertEquals(
            GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(appLinkReturnUrl)),
            sut(appLinkReturnUrl, fallbackSchemeUrl = "  ", checkoutUri = checkoutUri)
        )
    }

    @Test
    fun `Failure when both appLinkReturnUrl and fallbackSchemeUrl are blank`() {
        val result = sut(appLinkReturnUrl = "", fallbackSchemeUrl = "", checkoutUri = checkoutUri)

        assertTrue(result is GetReturnToAppStrategyResult.Failure)
        assertTrue((result as GetReturnToAppStrategyResult.Failure).error.isNotBlank())
    }

    @Test
    fun `Failure when both appLinkReturnUrl and fallbackSchemeUrl are whitespace`() {
        val result = sut(appLinkReturnUrl = "  ", fallbackSchemeUrl = "  ", checkoutUri = checkoutUri)

        assertTrue(result is GetReturnToAppStrategyResult.Failure)
    }

    @Test
    fun `uses the default checkout uri when none is provided`() {
        stubDefaultApp(appLinkReturnUri, merchantPackageName)
        // The stub matches the default checkout URI (example.com/checkout), which equals checkoutUri.
        stubDefaultApp(checkoutUri, "com.android.chrome")

        val expected = GetReturnToAppStrategyResult.Success(ReturnToAppStrategy.AppLink(appLinkReturnUrl))
        assertEquals(expected, sut(appLinkReturnUrl, fallbackScheme))
    }
}
