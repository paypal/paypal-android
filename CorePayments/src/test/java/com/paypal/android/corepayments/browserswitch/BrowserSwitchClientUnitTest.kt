package com.paypal.android.corepayments.browserswitch

import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserSwitchClientUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var chromeCustomTabsClient: ChromeCustomTabsClient
    private lateinit var sut: BrowserSwitchClient

    @Before
    fun beforeEach() {
        chromeCustomTabsClient = mockk<ChromeCustomTabsClient>(relaxed = true)
        sut = BrowserSwitchClient(chromeCustomTabsClient)
    }

    @Test
    fun `it should launch a chrome custom tab`() {
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/uri".toUri(),
            requestCode = 123,
            returnUrlScheme = "example.return.url.scheme"
        )
        sut.start(context, options)

        val expectedCCTOptions =
            ChromeCustomTabOptions(launchUri = "https://example.com/uri".toUri())
        verify { chromeCustomTabsClient.launch(context, expectedCCTOptions) }
    }
}