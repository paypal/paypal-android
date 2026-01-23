package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.common.DeviceInspector
import io.mockk.every
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

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val browserSwitchOptions = BrowserSwitchOptions(
        targetUri = "https://example.com/uri".toUri(),
        requestCode = 123,
        returnUrlScheme = "example.return.url.scheme",
        metadata = JSONObject().put("example_prop", "example_value"),
        appLinkUrl = null
    )

    private lateinit var chromeCustomTabsClient: ChromeCustomTabsClient
    private lateinit var authTabClient: AuthTabClient
    private lateinit var deviceInspector: DeviceInspector
    private lateinit var sut: BrowserSwitchClient

    @Before
    fun beforeEach() {
        chromeCustomTabsClient = mockk(relaxed = true)
        authTabClient = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)
        sut = BrowserSwitchClient(chromeCustomTabsClient, authTabClient, deviceInspector)
    }

    @Test
    fun `it should launch a chrome custom tab on success`() {
        every {
            deviceInspector.isDeepLinkConfiguredInManifest("example.return.url.scheme")
        } returns true
        every {
            chromeCustomTabsClient.launch(any(), any())
        } returns LaunchChromeCustomTabResult.Success

        val result = sut.start(appContext, browserSwitchOptions)
        val expectedCCTOptions =
            ChromeCustomTabOptions(launchUri = "https://example.com/uri".toUri())

        assertTrue(result is BrowserSwitchStartResult.Success)
        verify { chromeCustomTabsClient.launch(appContext, expectedCCTOptions) }
    }

    @Test
    fun `start with activity result launcher launches auth tab`() {
        every { deviceInspector.isAuthTabSupported } returns true

        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/auth".toUri(),
            requestCode = 123,
            returnUrlScheme = "com.example.app",
            metadata = JSONObject(),
            appLinkUrl = null
        )

        val result = sut.start(activityResultLauncher, options, appContext)

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
        every { deviceInspector.isAuthTabSupported } returns true

        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/auth".toUri(),
            requestCode = 123,
            returnUrlScheme = null,
            metadata = JSONObject(),
            appLinkUrl = "https://example.com/return"
        )

        val result = sut.start(activityResultLauncher, options, appContext)

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

    @Test
    fun `start with activity result launcher falls back to custom tabs when auth tabs not supported`() {
        every { deviceInspector.isAuthTabSupported } returns false
        every {
            deviceInspector.isDeepLinkConfiguredInManifest("com.example.app")
        } returns true
        every {
            chromeCustomTabsClient.launch(any(), any())
        } returns LaunchChromeCustomTabResult.Success

        val activityResultLauncher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)
        val options = BrowserSwitchOptions(
            targetUri = "https://example.com/auth".toUri(),
            requestCode = 123,
            returnUrlScheme = "com.example.app",
            metadata = JSONObject(),
            appLinkUrl = null
        )

        val result = sut.start(activityResultLauncher, options, appContext)

        assertTrue(result is BrowserSwitchStartResult.Success)
        // Verify it fell back to custom tabs instead of auth tab
        verify {
            chromeCustomTabsClient.launch(
                appContext,
                ChromeCustomTabOptions(launchUri = "https://example.com/auth".toUri())
            )
        }
        verify(exactly = 0) {
            authTabClient.launchAuthTab(any(), any(), any(), any())
        }
    }

    @Test
    fun `it should fail to launch when no browser activity is present on the device`() {
        every {
            deviceInspector.isDeepLinkConfiguredInManifest("example.return.url.scheme")
        } returns true
        every {
            chromeCustomTabsClient.launch(any(), any())
        } returns LaunchChromeCustomTabResult.ActivityNotFound

        val result = sut.start(appContext, browserSwitchOptions)
        assertTrue(result is BrowserSwitchStartResult.Failure)
        val message = (result as BrowserSwitchStartResult.Failure).error.message
        val expected = "Unable to launch Chrome Custom Tab on device without a web browser."
        assertEquals(expected, message)
    }

    @Test
    fun `it should fail to launch when the source activity context is finishing`() {
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        val activity = activityController.get()

        activity.finish()
        val result = sut.start(activity, browserSwitchOptions)
        assertTrue(result is BrowserSwitchStartResult.Failure)
        val message = (result as BrowserSwitchStartResult.Failure).error.message
        val expected = "Unable to launch Chrome Custom Tab while the source Activity is finishing."
        assertEquals(expected, message)
    }

    @Test
    fun `it should fail to launch when the AndroidManifest is not properly configured for deep linking`() {
        every {
            deviceInspector.isDeepLinkConfiguredInManifest("example.return.url.scheme")
        } returns false

        val result = sut.start(appContext, browserSwitchOptions)
        assertTrue(result is BrowserSwitchStartResult.Failure)
        val message = (result as BrowserSwitchStartResult.Failure).error.message
        val expected = "This app is not correctly configured to handle deep links from the return url scheme provided."
        assertEquals(expected, message)
    }

    @Test
    fun `it should fail when return url scheme and app link url are both null`() {
        val invalidOptions = BrowserSwitchOptions(
            targetUri = "https://example.com/uri".toUri(),
            requestCode = 123,
            returnUrlScheme = null,
            metadata = JSONObject().put("example_prop", "example_value"),
            appLinkUrl = null
        )
        val result = sut.start(appContext, invalidOptions)
        assertTrue(result is BrowserSwitchStartResult.Failure)
        val message = (result as BrowserSwitchStartResult.Failure).error.message
        val expected = "The properties 'returnUrlScheme' and 'appLinkUrl' cannot both be null."
        assertEquals(expected, message)
    }
}
