package com.paypal.android.corepayments.common

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `isPayPalInstalled returns false when PayPal app is installed but disabled`() {
        // Regression coverage: on real devices, "uninstalling" a preloaded/system app is often
        // actually just disabling it for the user (`pm disable-user`), which does NOT throw
        // NameNotFoundException from getApplicationInfo() — it returns an ApplicationInfo with
        // enabled == false. Treating that as "installed" would incorrectly attempt an app
        // switch into an app that can't actually be launched.
        every {
            packageManager.getApplicationInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } returns ApplicationInfo().apply { enabled = false }

        val result = sut.isPayPalInstalled

        assertFalse(result)
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
    fun `payPalAppVersionName returns the installed PayPal app's versionName`() {
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } returns PackageInfo().apply { versionName = "10.1.0" }

        val result = sut.payPalAppVersionName

        assertEquals("10.1.0", result)
    }

    @Test
    fun `payPalAppVersionName returns null when PayPal app is not installed`() {
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } throws PackageManager.NameNotFoundException("Package not found")

        val result = sut.payPalAppVersionName

        assertNull(result)
    }

    @Test
    fun `payPalAppVersionName returns null when PackageManager throws unexpected exception`() {
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } throws RuntimeException("Unexpected error")

        val result = sut.payPalAppVersionName

        assertNull(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch returns true when the PayPal app resolves the uri and meets the min version`() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = DeviceInspector.PAYPAL_APP_PACKAGE }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } returns PackageInfo().apply { versionName = "9.1.0" }

        val result = sut.canResolvePayPalAppSwitch()

        assertTrue(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch returns true when installed version is exactly the minimum major version`() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = DeviceInspector.PAYPAL_APP_PACKAGE }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } returns PackageInfo().apply { versionName = "9.0.0" }

        val result = sut.canResolvePayPalAppSwitch()

        assertTrue(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch returns false when the resolved app's version is below the minimum`() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = DeviceInspector.PAYPAL_APP_PACKAGE }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } returns PackageInfo().apply { versionName = "8.9.9" }

        val result = sut.canResolvePayPalAppSwitch()

        assertFalse(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch returns false when the resolved app's version can't be determined`() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = DeviceInspector.PAYPAL_APP_PACKAGE }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } throws PackageManager.NameNotFoundException("Package not found")

        val result = sut.canResolvePayPalAppSwitch()

        assertFalse(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch returns false when the resolved app's version has an unparseable major segment`() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = DeviceInspector.PAYPAL_APP_PACKAGE }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo
        every {
            packageManager.getPackageInfo(DeviceInspector.PAYPAL_APP_PACKAGE, 0)
        } returns PackageInfo().apply { versionName = "not-a-version" }

        val result = sut.canResolvePayPalAppSwitch()

        assertFalse(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch returns false when a browser resolves the app-switch uri instead`() {
        // Regression coverage: the PayPal app can be installed and enabled but still lose out to
        // a browser if the user has unchecked "Open supported links" for it (Android App Links
        // settings) — in that case resolveActivity() resolves to the default browser, not PayPal.
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply { packageName = "com.android.chrome" }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        val result = sut.canResolvePayPalAppSwitch()

        assertFalse(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch returns false when no activity resolves the app-switch uri`() {
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns null

        val result = sut.canResolvePayPalAppSwitch()

        assertFalse(result)
    }

    @Test
    fun `canResolvePayPalAppSwitch queries resolveActivity with the given uri and BROWSABLE category`() {
        val intentSlot = slot<Intent>()
        every {
            packageManager.resolveActivity(capture(intentSlot), PackageManager.MATCH_DEFAULT_ONLY)
        } returns null

        val uri = Uri.parse("https://www.paypal.com/agreements/approve")
        sut.canResolvePayPalAppSwitch(uri)

        val capturedIntent = intentSlot.captured
        assertEquals(Intent.ACTION_VIEW, capturedIntent.action)
        assertEquals(uri, capturedIntent.data)
        assertTrue(capturedIntent.hasCategory(Intent.CATEGORY_BROWSABLE))
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
}
