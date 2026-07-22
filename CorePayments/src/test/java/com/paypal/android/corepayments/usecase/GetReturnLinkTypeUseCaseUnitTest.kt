package com.paypal.android.corepayments.usecase

import android.content.Context
import android.net.Uri
import com.paypal.android.corepayments.LinkType
import com.paypal.android.corepayments.common.DeviceInspector
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("MaxLineLength")
@RunWith(RobolectricTestRunner::class)
class GetReturnLinkTypeUseCaseUnitTest {

    private val applicationContext: Context = mockk()
    private val deviceInspector: DeviceInspector = mockk()
    private val getDefaultAppUseCase: GetDefaultAppUseCase = mockk()
    private val hasAppLinksCompatibleBrowserUseCase: HasAppLinksCompatibleBrowserUseCase = mockk()

    private val merchantPackageName = "com.merchant.app"
    private val appLinkReturnUrl = "https://merchant.com/return"
    private val appLinkReturnUri: Uri = Uri.parse(appLinkReturnUrl)
    private val checkoutUri: Uri = Uri.parse("https://www.paypal.com/checkout")
    private val fallbackScheme = "com.merchant.app.paypal"

    private lateinit var sut: GetReturnLinkTypeUseCase

    @Before
    fun beforeEach() {
        every { applicationContext.packageName } returns merchantPackageName
        every { deviceInspector.canResolvePayPalAppSwitch() } returns false
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false
        sut = GetReturnLinkTypeUseCase(
            applicationContext,
            deviceInspector,
            getDefaultAppUseCase,
            hasAppLinksCompatibleBrowserUseCase,
        )
    }

    @Test
    fun `APP_LINK when merchant is default handler and an App-Links-compatible browser is available`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns merchantPackageName
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns true

        assertEquals(LinkType.APP_LINK, sut(appLinkReturnUrl, fallbackScheme, checkoutUri))
    }

    @Test
    fun `APP_LINK when merchant is default handler and the PayPal app can app switch`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns merchantPackageName
        every { deviceInspector.canResolvePayPalAppSwitch() } returns true
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false

        assertEquals(LinkType.APP_LINK, sut(appLinkReturnUrl, fallbackScheme, checkoutUri))
    }

    @Test
    fun `DEEP_LINK when merchant is default handler but no compatible browser or first-party app`() {
        // AC1: default links disabled at the browser level -> fall back to deep link
        every { getDefaultAppUseCase(appLinkReturnUri) } returns merchantPackageName
        every { deviceInspector.canResolvePayPalAppSwitch() } returns false
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false

        assertEquals(LinkType.DEEP_LINK, sut(appLinkReturnUrl, fallbackScheme, checkoutUri))
    }

    @Test
    fun `DEEP_LINK when merchant app is not the default handler for its own return uri`() {
        // AC1: "Open supported links" unchecked for the merchant app -> not verified -> deep link
        every { getDefaultAppUseCase(appLinkReturnUri) } returns "com.other.app"
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns true

        assertEquals(LinkType.DEEP_LINK, sut(appLinkReturnUrl, fallbackScheme, checkoutUri))
    }

    @Test
    fun `DEEP_LINK when the app link return url is null`() {
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns true

        assertEquals(
            LinkType.DEEP_LINK,
            sut(appLinkReturnUrl = null, fallbackSchemeUrl = fallbackScheme, checkoutUri = checkoutUri)
        )
    }

    @Test
    fun `APP_LINK when App Links won't route but no fallback scheme is available`() {
        // Deep-link conditions, but there is no custom scheme to switch to -> App Link.
        every { getDefaultAppUseCase(appLinkReturnUri) } returns "com.other.app"
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false

        assertEquals(
            LinkType.APP_LINK,
            sut(appLinkReturnUrl, fallbackSchemeUrl = null, checkoutUri = checkoutUri)
        )
    }

    @Test
    fun `APP_LINK when App Links won't route but fallback scheme is blank`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns "com.other.app"
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false

        assertEquals(
            LinkType.APP_LINK,
            sut(appLinkReturnUrl, fallbackSchemeUrl = "  ", checkoutUri = checkoutUri)
        )
    }

    @Test
    fun `uses the default checkout uri when none is provided`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns merchantPackageName
        // The stub matches the default checkout URI (paypal.com/checkout), which equals checkoutUri.
        every { hasAppLinksCompatibleBrowserUseCase(checkoutUri) } returns true

        assertEquals(LinkType.APP_LINK, sut(appLinkReturnUrl, fallbackScheme))
    }
}
