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
    private val getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase = mockk()

    private val merchantPackageName = "com.merchant.app"
    private val appLinkReturnUri: Uri = Uri.parse("https://merchant.com/return")
    private val checkoutUri: Uri = Uri.parse("https://www.paypal.com/checkout")

    private lateinit var sut: GetReturnLinkTypeUseCase

    @Before
    fun beforeEach() {
        every { applicationContext.packageName } returns merchantPackageName
        every { deviceInspector.canResolvePayPalAppSwitch() } returns false
        every { getAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false
        sut = GetReturnLinkTypeUseCase(
            applicationContext,
            deviceInspector,
            getDefaultAppUseCase,
            getAppLinksCompatibleBrowserUseCase,
        )
    }

    @Test
    fun `APP_LINK when merchant is default handler and an App-Links-compatible browser is available`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns merchantPackageName
        every { getAppLinksCompatibleBrowserUseCase(checkoutUri) } returns true

        assertEquals(LinkType.APP_LINK, sut(appLinkReturnUri, checkoutUri))
    }

    @Test
    fun `APP_LINK when merchant is default handler and the PayPal app can app switch`() {
        every { getDefaultAppUseCase(appLinkReturnUri) } returns merchantPackageName
        every { deviceInspector.canResolvePayPalAppSwitch() } returns true
        every { getAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false

        assertEquals(LinkType.APP_LINK, sut(appLinkReturnUri, checkoutUri))
    }

    @Test
    fun `DEEP_LINK when merchant is default handler but no compatible browser or first-party app`() {
        // AC1: default links disabled at the browser level -> fall back to deep link
        every { getDefaultAppUseCase(appLinkReturnUri) } returns merchantPackageName
        every { deviceInspector.canResolvePayPalAppSwitch() } returns false
        every { getAppLinksCompatibleBrowserUseCase(checkoutUri) } returns false

        assertEquals(LinkType.DEEP_LINK, sut(appLinkReturnUri, checkoutUri))
    }

    @Test
    fun `DEEP_LINK when merchant app is not the default handler for its own return uri`() {
        // AC1: "Open supported links" unchecked for the merchant app -> not verified -> deep link
        every { getDefaultAppUseCase(appLinkReturnUri) } returns "com.other.app"
        every { getAppLinksCompatibleBrowserUseCase(checkoutUri) } returns true

        assertEquals(LinkType.DEEP_LINK, sut(appLinkReturnUri, checkoutUri))
    }

    @Test
    fun `DEEP_LINK when the app link return uri is null`() {
        every { getAppLinksCompatibleBrowserUseCase(checkoutUri) } returns true

        assertEquals(LinkType.DEEP_LINK, sut(appLinkReturnUri = null, checkoutUri = checkoutUri))
    }
}
