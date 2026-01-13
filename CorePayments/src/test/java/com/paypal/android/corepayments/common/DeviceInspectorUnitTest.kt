package com.paypal.android.corepayments.common

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
    fun `isDeepLinkConfiguredInManifest returns false by default`() {
        val result = sut.isDeepLinkConfiguredInManifest(context, "com.example.app.returnscheme")
        assertFalse(result)
    }

    @Test
    fun `isDeepLinkConfiguredInManifest queries for intent with correct action, data, and categories`() {
        val intentSlot = slot<Intent>()
        every {
            packageManager.queryIntentActivities(capture(intentSlot), 0)
        } returns listOf(ResolveInfo())

        sut.isDeepLinkConfiguredInManifest(context, "com.example.app.returnscheme")

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

        val result = sut.isDeepLinkConfiguredInManifest(context, "com.example.app.returnscheme")
        assertTrue(result)
    }
}
