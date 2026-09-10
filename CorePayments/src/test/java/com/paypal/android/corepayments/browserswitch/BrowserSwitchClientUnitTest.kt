package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
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
    fun `it should launch an auth tab for auth tab options`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).get()
        val authTabOptions = browserSwitchOptions.copy(
            launchMode = BrowserSwitchLaunchMode.AUTH_TAB,
        )
        every {
            deviceInspector.isDeepLinkConfiguredInManifest("example.return.url.scheme")
        } returns true
        every { authTabClient.launch(activity, authTabOptions) } returns LaunchAuthTabResult.Success

        val result = sut.start(activity, authTabOptions)

        assertTrue(result is BrowserSwitchStartResult.Success)
        verify { authTabClient.launch(activity, authTabOptions) }
        verify(exactly = 0) { chromeCustomTabsClient.launch(any(), any()) }
    }

    @Test
    fun `it should reject auth tab launch without an activity`() {
        val authTabOptions = browserSwitchOptions.copy(launchMode = BrowserSwitchLaunchMode.AUTH_TAB)
        every {
            deviceInspector.isDeepLinkConfiguredInManifest("example.return.url.scheme")
        } returns true

        val result = sut.start(appContext, authTabOptions)

        assertTrue(result is BrowserSwitchStartResult.Failure)
        val message = (result as BrowserSwitchStartResult.Failure).error.message
        assertEquals("Unable to launch Auth Tab without a source Activity.", message)
        verify(exactly = 0) { authTabClient.launch(any(), any()) }
    }

    @Test
    fun `it should fall back to a chrome custom tab for auth tab options from a plain Activity`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val authTabOptions = browserSwitchOptions.copy(launchMode = BrowserSwitchLaunchMode.AUTH_TAB)
        every {
            deviceInspector.isDeepLinkConfiguredInManifest("example.return.url.scheme")
        } returns true
        every {
            chromeCustomTabsClient.launch(any(), any())
        } returns LaunchChromeCustomTabResult.Success

        val result = sut.start(activity, authTabOptions)
        val expectedCCTOptions =
            ChromeCustomTabOptions(launchUri = "https://example.com/uri".toUri())

        assertTrue(result is BrowserSwitchStartResult.Success)
        verify { chromeCustomTabsClient.launch(activity, expectedCCTOptions) }
        verify(exactly = 0) { authTabClient.launch(any(), any()) }
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
    fun `it should reject auth tab launch when the App Link receiver is not configured`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).get()
        val authTabOptions = browserSwitchOptions.copy(
            returnUrlScheme = null,
            appLinkUrl = "https://merchant.example/return",
            launchMode = BrowserSwitchLaunchMode.AUTH_TAB,
        )
        every {
            deviceInspector.isAppLinkConfiguredInManifest("https://merchant.example/return")
        } returns false

        val result = sut.start(activity, authTabOptions)

        assertTrue(result is BrowserSwitchStartResult.Failure)
        val message = (result as BrowserSwitchStartResult.Failure).error.message
        assertEquals(
            "This app is not correctly configured to handle the provided App Link return URL.",
            message,
        )
        verify(exactly = 0) { authTabClient.launch(any(), any()) }
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
