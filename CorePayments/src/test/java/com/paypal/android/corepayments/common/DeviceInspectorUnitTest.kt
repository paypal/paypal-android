package com.paypal.android.corepayments.common

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.browser.customtabs.CustomTabsClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceInspectorUnitTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var sut: DeviceInspector

    @Before
    fun beforeEach() {
        context = mockk()
        packageManager = mockk()
        every { context.packageManager } returns packageManager
        sut = DeviceInspector(context)
    }

    @After
    fun afterEach() {
        unmockkStatic(CustomTabsClient::class)
    }

    @Test
    fun `isPayPalInstalled returns true when PayPal app is installed`() {
        every {
            packageManager.getApplicationInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } returns ApplicationInfo()

        val result = sut.isPayPalInstalled

        assertTrue(result)
    }

    @Test
    fun `isPayPalInstalled returns false when PayPal app is not installed`() {
        every {
            packageManager.getApplicationInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } throws PackageManager.NameNotFoundException("Package not found")

        val result = sut.isPayPalInstalled

        assertFalse(result)
    }

    @Test
    fun `isPayPalInstalled handles PackageManager exceptions gracefully`() {
        every {
            packageManager.getApplicationInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } throws RuntimeException("Unexpected error")

        val result = sut.isPayPalInstalled

        assertFalse(result)
    }

    @Test
    fun `isDeepLinkConfiguredInManifest queries for intent with correct action, data, and categories`() {
        val intentSlot = slot<Intent>()
        every {
            packageManager.queryIntentActivities(capture(intentSlot), 0)
        } returns listOf(ResolveInfo())

        sut.isDeepLinkConfiguredInManifest("com.example.app.returnscheme")

        val capturedIntent = intentSlot.captured
        assertEquals(Intent.ACTION_VIEW, capturedIntent.action)
        assertEquals("com.example.app.returnscheme://", capturedIntent.data.toString())
        assertTrue(capturedIntent.hasCategory(Intent.CATEGORY_DEFAULT))
        assertTrue(capturedIntent.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `isDeepLinkConfiguredInManifest returns true when deep link is configured in manifest`() {
        every {
            packageManager.queryIntentActivities(any<Intent>(), 0)
        } returns listOf(ResolveInfo())

        val result = sut.isDeepLinkConfiguredInManifest("com.example.app.returnscheme")
        assertTrue(result)
    }

    @Test
    fun `isDeepLinkConfiguredInManifest returns false when no matching activities found`() {
        every {
            packageManager.queryIntentActivities(any<Intent>(), 0)
        } returns emptyList()

        val result = sut.isDeepLinkConfiguredInManifest("com.example.app.returnscheme")
        assertFalse(result)
    }

    // Tests for isAuthTabSupported

    @Test
    fun `isAuthTabSupported returns true when default browser supports auth tabs`() {
        mockkStatic(CustomTabsClient::class)

        val resolveInfo = mockk<ResolveInfo>()
        val activityInfo = mockk<ActivityInfo>()
        resolveInfo.activityInfo = activityInfo
        activityInfo.packageName = "com.android.chrome"

        every {
            packageManager.resolveActivity(any<Intent>(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        every {
            CustomTabsClient.isAuthTabSupported(context, "com.android.chrome")
        } returns true

        val result = sut.isAuthTabSupported

        assertTrue(result)
    }

    @Test
    fun `isAuthTabSupported returns false when default browser does not support auth tabs`() {
        mockkStatic(CustomTabsClient::class)

        val resolveInfo = mockk<ResolveInfo>()
        val activityInfo = mockk<ActivityInfo>()
        resolveInfo.activityInfo = activityInfo
        activityInfo.packageName = "com.android.chrome"

        every {
            packageManager.resolveActivity(any<Intent>(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        every {
            CustomTabsClient.isAuthTabSupported(context, "com.android.chrome")
        } returns false

        val result = sut.isAuthTabSupported

        assertFalse(result)
    }

    @Test
    fun `isAuthTabSupported returns false when no default browser is set`() {
        mockkStatic(CustomTabsClient::class)

        every {
            packageManager.resolveActivity(any<Intent>(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns null

        val result = sut.isAuthTabSupported

        assertFalse(result)
        verify(exactly = 0) {
            CustomTabsClient.isAuthTabSupported(any(), any())
        }
    }
}
