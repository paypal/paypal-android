package com.paypal.android.corepayments.browserswitch

import android.content.Context
import androidx.activity.ComponentActivity
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserSwitchClientUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val browserSwitchOptions = BrowserSwitchOptions(
        targetUri = "https://example.com/uri".toUri(),
        requestCode = 123,
        returnUrlScheme = "example.return.url.scheme",
        metadata = JSONObject().put("example_prop", "example_value"),
        appLinkUrl = null
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
    fun `it should fail to launch when the source activity context is finishing`() {
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity = activityController.get()

        activity.finish()
        val result = sut.start(context, browserSwitchOptions)
        assertTrue(result is BrowserSwitchStartResult.Failure)
        val message = (result as BrowserSwitchStartResult.Failure).error.message
        val expected = "Unable to launch Chrome Custom Tab while the source Activity is finishing."
        assertEquals(expected, message)
    }
}
