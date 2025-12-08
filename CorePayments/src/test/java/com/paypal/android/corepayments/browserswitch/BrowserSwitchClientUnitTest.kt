package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class BrowserSwitchClientUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val browserSwitchOptions = BrowserSwitchOptions(
        targetUri = "https://example.com/uri".toUri(),
        requestCode = 123,
        returnUrlScheme = "example.return.url.scheme",
        metadata = JSONObject().put("example_prop", "example_value")
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
    fun `it should finish unsuccessfully when the request code does not match the input pending state`() {
        val intent = Intent()
        val pendingState = BrowserSwitchPendingState(browserSwitchOptions)

        val result = sut.finish(intent, 456, pendingState)
        assertTrue(result is BrowserSwitchFinishResult.RequestCodeDoesNotMatch)
    }

    @Test
    fun `it should finish unsuccessfully when the input intent has no deep link`() {
        val intent = Intent().apply {
            data = null
        }
        val pendingState = BrowserSwitchPendingState(browserSwitchOptions)
        val result = sut.finish(intent, 123, pendingState)
        assertTrue(result is BrowserSwitchFinishResult.DeepLinkNotPresent)
    }

    @Test
    fun `it should finish unsuccessfully when the input intent deep link has an unrecognized custom url scheme`() {
        val intent = Intent().apply {
            data = "unrecognized.return.url.scheme://domain/path".toUri()
        }
        val pendingState = BrowserSwitchPendingState(browserSwitchOptions)
        val result = sut.finish(intent, 123, pendingState)
        assertTrue(result is BrowserSwitchFinishResult.DeepLinkDoesNotMatch)
    }

    @Test
    fun `it should finish successfully when the input intent deep link has a matching custom url scheme`() {
        val intent = Intent().apply {
            data = "example.return.url.scheme://domain/path".toUri()
        }
        val pendingState = BrowserSwitchPendingState(browserSwitchOptions)
        val result = sut.finish(intent, 123, pendingState)

        val successResult = result as? BrowserSwitchFinishResult.Success
        assertEquals("example.return.url.scheme://domain/path".toUri(), successResult?.deepLinkUri)
        assertEquals(123, successResult?.requestCode)
        assertEquals("https://example.com/uri".toUri(), successResult?.requestUrl)
        JSONAssert.assertEquals(
            JSONObject().put("example_prop", "example_value"),
            successResult?.requestMetadata,
            false
        )
    }

    @Test
    fun `it should finish successfully when the input intent deep link has a case-insensitive matching custom url scheme`() {
        val intent = Intent().apply {
            data = "EXAMPLE.RETURN.URL.SCHEME://domain/path".toUri()
        }
        val pendingState = BrowserSwitchPendingState(browserSwitchOptions)
        val result = sut.finish(intent, 123, pendingState)

        val successResult = result as? BrowserSwitchFinishResult.Success
        assertEquals("EXAMPLE.RETURN.URL.SCHEME://domain/path".toUri(), successResult?.deepLinkUri)
        assertEquals(123, successResult?.requestCode)
        assertEquals("https://example.com/uri".toUri(), successResult?.requestUrl)
        JSONAssert.assertEquals(
            JSONObject().put("example_prop", "example_value"),
            successResult?.requestMetadata,
            false
        )
    }
}
