package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
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
        returnUrlScheme = "example.return.url.scheme",
        metadata = JSONObject().put("example_prop", "example_value"),
        appLinkUrl = null
    )

    private lateinit var chromeCustomTabsClient: ChromeCustomTabsClient
    private lateinit var authTabClient: AuthTabClient
    private lateinit var sut: BrowserSwitchClient

    @Before
    fun beforeEach() {
        chromeCustomTabsClient = mockk<ChromeCustomTabsClient>(relaxed = true)
        authTabClient = mockk<AuthTabClient>(relaxed = true)
        sut = BrowserSwitchClient(chromeCustomTabsClient, authTabClient)
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
    fun `start with activity result launcher launches auth tab`() {
        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/auth".toUri(),
            requestCode = 123,
            returnUrlScheme = "com.example.app",
            metadata = JSONObject(),
            appLinkUrl = null
        )

        val result = sut.start(activityResultLauncher, options)

        assertTrue(result is BrowserSwitchStartResult.Success)
        verify {
            authTabClient.launchAuthTab(
                options = ChromeCustomTabOptions(launchUri = "https://example.com/auth".toUri()),
                activityResultLauncher = activityResultLauncher,
                appLinkUrl = null,
                returnUrlScheme = "com.example.app"
            )
        }
    }

    @Test
    fun `start with activity result launcher and appLinkUrl passes appLinkUrl to auth tab`() {
        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/auth".toUri(),
            requestCode = 123,
            returnUrlScheme = null,
            metadata = JSONObject(),
            appLinkUrl = "https://example.com/return"
        )

        val result = sut.start(activityResultLauncher, options)

        assertTrue(result is BrowserSwitchStartResult.Success)
        verify {
            authTabClient.launchAuthTab(
                options = ChromeCustomTabOptions(launchUri = "https://example.com/auth".toUri()),
                activityResultLauncher = activityResultLauncher,
                appLinkUrl = "https://example.com/return",
                returnUrlScheme = null
            )
        }
    }
}
