package com.paypal.android.corepayments.analytics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceInspectorUnitTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    private lateinit var packageInfo: PackageInfo
    private lateinit var applicationInfo: ApplicationInfo

    @Before
    fun beforeEach() {
        context = mockk()
        packageManager = mockk()

        every { context.packageManager } returns packageManager
        every { context.packageName } returns "sample.package.name"

        applicationInfo = ApplicationInfo()
        every {
            packageManager.getApplicationInfo("sample.package.name", 0)
        } returns applicationInfo

        every {
            packageManager.getApplicationLabel(applicationInfo)
        } returns "sample-application-label"

        packageInfo = PackageInfo()
        packageInfo.versionName = "7.8.9"

        every {
            packageManager.getPackageInfo("sample.package.name", 0)
        } returns packageInfo
    }

    @Test
    fun `inspect properly inspects context and build info to construct DeviceData`() {
        val sut = DeviceInspector(
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "sample-fingerprint",
            context = context
        )
        val result = sut.inspect()

        assertEquals(result.appId, "sample.package.name")
        assertEquals(result.appName, "sample-application-label")
        assertEquals(result.clientOS, "Android API 456")
        assertEquals(result.clientSDKVersion, "1.2.3")
        assertEquals(result.deviceManufacturer, "sample-manufacturer")
        assertEquals(result.deviceModel, "sample-model")
        assertEquals(result.isSimulator, false)
        assertEquals(result.merchantAppVersion, "7.8.9")
    }

    @Test
    fun `inspect returns true for isSimulator when device product is google_sdk`() {
        val sut = DeviceInspector(
            context = context,
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "google_sdk",
            deviceFingerprint = "sample-fingerprint"
        )
        val result = sut.inspect()

        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns true for isSimulator when device product is sdk`() {
        val sut = DeviceInspector(
            context = context,
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "google_sdk",
            deviceFingerprint = "sample-fingerprint"
        )
        val result = sut.inspect()
        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns true for isSimulator when device manufacturer is Genymotion`() {
        val sut = DeviceInspector(
            context = context,
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "Genymotion",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "sample-fingerprint"
        )
        val result = sut.inspect()
        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns true for isSimulator when device fingerprint contains generic`() {
        val sut = DeviceInspector(
            context = context,
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "some-generic-fingerprint"
        )
        val result = sut.inspect()
        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns Not Applicable for app name when package manager throws`() {
        val nameNotFound = PackageManager.NameNotFoundException("get application info error")
        every {
            packageManager.getApplicationInfo("sample.package.name", 0)
        } throws nameNotFound

        val sut = DeviceInspector(
            context = context,
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "some-generic-fingerprint"
        )
        val result = sut.inspect()
        assertEquals("N/A", result.appName)
    }

    @Test
    fun `inspect returns Not Applicable for merchant app version when package manager throws`() {
        val nameNotFound = PackageManager.NameNotFoundException("get package info error")
        every {
            packageManager.getPackageInfo("sample.package.name", 0)
        } throws nameNotFound

        val sut = DeviceInspector(
            context = context,
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "some-generic-fingerprint"
        )
        val result = sut.inspect()

        assertEquals("N/A", result.merchantAppVersion)
    }
}
