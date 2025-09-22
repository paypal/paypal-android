package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserSwitchClientUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val browserSwitchOptions = BrowserSwitchOptions(
        targetUri = "https://example.com/uri".toUri(),
        requestCode = 123,
        returnUrlScheme = "example.return.url.scheme"
    )

    private lateinit var chromeCustomTabsClient: ChromeCustomTabsClient
    private lateinit var sut: BrowserSwitchClient

    @Before
    fun beforeEach() {
        chromeCustomTabsClient = mockk<ChromeCustomTabsClient>(relaxed = true)
        sut = BrowserSwitchClient(chromeCustomTabsClient)
    }

    @Test
    fun `it should launch a chrome custom tab on success`() {
        val result = sut.start(context, browserSwitchOptions)
        val expectedCCTOptions =
            ChromeCustomTabOptions(launchUri = "https://example.com/uri".toUri())

        assertTrue(result is BrowserSwitchStartResult.Success)
        verify { chromeCustomTabsClient.launch(context, expectedCCTOptions) }
    }

    @Test
    fun `it should return browser switch pending state after a successful browser switch start`() {
        val result = sut.start(context, browserSwitchOptions)
        val pendingState = (result as? BrowserSwitchStartResult.Success)?.pendingState

        val expectedPendingState = BrowserSwitchPendingState(browserSwitchOptions)
        assertEquals(expectedPendingState, pendingState)
    }

    @Test
    fun `it should finish with failure when the request code does not match the input pending state`() {
        val intent = Intent()
        val pendingState = BrowserSwitchPendingState(browserSwitchOptions)
        val result = sut.finish(intent, 456, pendingState)
        assertTrue(result is BrowserSwitchFinishResult.RequestCodeDoesNotMatch)
    }
}