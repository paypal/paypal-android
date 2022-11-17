package com.paypal.android.core.analytics

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

    lateinit var context: Context
    lateinit var packageManager: PackageManager

    lateinit var packageInfo: PackageInfo
    lateinit var applicationInfo: ApplicationInfo

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
            deviceFingerprint = "sample-fingerprint"
        )
        val result = sut.inspect(context)

        // TODO: - Implement these asserts with either regex or appropriate hard coded actual cases. See iOS AnalyticsEventRequest_Tests for reference.
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
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "google_sdk",
            deviceFingerprint = "sample-fingerprint"
        )

        val result = sut.inspect(context)
        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns true for isSimulator when device product is sdk`() {
        val sut = DeviceInspector(
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "google_sdk",
            deviceFingerprint = "sample-fingerprint"
        )

        val result = sut.inspect(context)
        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns true for isSimulator when device manufacturer is Genymotion`() {
        val sut = DeviceInspector(
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "Genymotion",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "sample-fingerprint"
        )

        val result = sut.inspect(context)
        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns true for isSimulator when device fingerprint contains generic`() {
        val sut = DeviceInspector(
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "some-generic-fingerprint"
        )

        val result = sut.inspect(context)
        assertTrue(result.isSimulator)
    }

    @Test
    fun `inspect returns an empty string for app name when package manager throws`() {
        val nameNotFound = PackageManager.NameNotFoundException("get application info error")
        every {
            packageManager.getApplicationInfo("sample.package.name", 0)
        } throws nameNotFound

        val sut = DeviceInspector(
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "some-generic-fingerprint"
        )

        val result = sut.inspect(context)
        assertEquals("", result.appName)
    }

    @Test
    fun `inspect returns an empty string for merchant app version when package manager throws`() {
        val nameNotFound = PackageManager.NameNotFoundException("get package info error")
        every {
            packageManager.getPackageInfo("sample.package.name", 0)
        } throws nameNotFound

        val sut = DeviceInspector(
            clientSDKVersion = "1.2.3",
            sdkInt = 456,
            deviceManufacturer = "sample-manufacturer",
            deviceModel = "sample-model",
            deviceProduct = "sample-product",
            deviceFingerprint = "some-generic-fingerprint"
        )

        val result = sut.inspect(context)
        assertEquals("", result.merchantAppVersion)
    }
}