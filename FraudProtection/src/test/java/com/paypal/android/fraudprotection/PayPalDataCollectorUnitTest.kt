package com.paypal.android.fraudprotection

import android.content.Context
import android.util.Log
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import lib.android.paypal.com.magnessdk.InvalidInputException
import lib.android.paypal.com.magnessdk.MagnesResult
import lib.android.paypal.com.magnessdk.MagnesSDK
import lib.android.paypal.com.magnessdk.MagnesSettings
import lib.android.paypal.com.magnessdk.MagnesSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class PayPalDataCollectorUnitTest {

    private val sandboxConfig = CoreConfig("fake-client-id", Environment.SANDBOX)
    private val liveConfig = CoreConfig("fake-client-id", Environment.LIVE)

    private lateinit var context: Context

    @Before
    fun beforeEach() {
        context = mockk(relaxed = true)
    }

    @Test
    fun `when environment is SANDBOX, magnes settings environment is STAGE`() {
        val appGUID = UUID.randomUUID().toString()
        val mockMagnesSDK = mockk<MagnesSDK>(relaxed = true)
        val mockUUIDHelper = mockk<UUIDHelper>(relaxed = true)
        val magnesSettingsSlot = slot<MagnesSettings>()

        every { mockUUIDHelper.getInstallationGUID(any()) } returns appGUID
        val sut = PayPalDataCollector(sandboxConfig, mockMagnesSDK, mockUUIDHelper)
        sut.collectDeviceData(context, PayPalDataCollectorRequest(hasUserLocationConsent = false))
        verify { mockMagnesSDK.setUp(capture(magnesSettingsSlot)) }

        val magnesSettings = magnesSettingsSlot.captured
        assertEquals(
            magnesSettings.environment,
            lib.android.paypal.com.magnessdk.Environment.SANDBOX
        )
        assertFalse(magnesSettings.isDisableBeacon)
        assertEquals(magnesSettings.appGuid, appGUID)
        assertEquals(magnesSettings.magnesSource, MagnesSource.PAYPAL.version)
    }

    @Test
    fun `when environment is LIVE, magnes settings environment is LIVE`() {
        val appGUID = UUID.randomUUID().toString()
        val mockMagnesSDK = mockk<MagnesSDK>(relaxed = true)
        val mockUUIDHelper = mockk<UUIDHelper>(relaxed = true)
        val magnesSettingsSlot = slot<MagnesSettings>()

        every { mockUUIDHelper.getInstallationGUID(any()) } returns appGUID

        val sut = PayPalDataCollector(liveConfig, mockMagnesSDK, mockUUIDHelper)
        sut.collectDeviceData(context, PayPalDataCollectorRequest(hasUserLocationConsent = false))

        verify { mockMagnesSDK.setUp(capture(magnesSettingsSlot)) }

        val magnesSettings = magnesSettingsSlot.captured
        assertEquals(magnesSettings.environment, lib.android.paypal.com.magnessdk.Environment.LIVE)
    }

    @Test
    fun `when appGUID is invalid, InvalidInputException is thrown`() {
        mockkStatic(Log::class)
        val errorMessage =
            "Application’s Globally Unique Identifier (AppGUID) does not match the criteria," +
                    " This is a string that identifies the merchant application that sets up Magnes on the mobile" +
                    " device. If the merchant app does not pass an AppGuid, Magnes creates one to identify" +
                    " the app. An AppGuid is an application identifier per-installation; that is," +
                    " if a new instance of the app is installed on the mobile device, or the app" +
                    " is reinstalled, it will have a new AppGuid.\n ***AppGuid Criteria*** \n   " +
                    "Max length: 36 characters \n   Min Length: 30 characters \n   " +
                    "Regex: Letters, numbers and dashes only \n"
        val appGUID = "invalid_uuid"
        val mockMagnesSDK = mockk<MagnesSDK>(relaxed = true)
        val mockUUIDHelper = mockk<UUIDHelper>(relaxed = true)
        val exceptionSlot = slot<InvalidInputException>()

        every { mockUUIDHelper.getInstallationGUID(any()) } returns appGUID

        val sut = PayPalDataCollector(sandboxConfig, mockMagnesSDK, mockUUIDHelper)
        val result = sut.collectDeviceData(
            context,
            PayPalDataCollectorRequest(hasUserLocationConsent = false)
        )

        verify { Log.e(any(), any(), capture(exceptionSlot)) }

        assertEquals(exceptionSlot.captured.message, errorMessage)
        assertEquals("", result)
    }

    @Test
    fun `when collectDeviceData is called with correct values, it returns client metadata`() {
        mockkStatic(Log::class)
        val appGUID = UUID.randomUUID().toString()
        val clientMetadataId = "client_metadata_id"
        val mockMagnesSDK = mockk<MagnesSDK>(relaxed = true)
        val mockUUIDHelper = mockk<UUIDHelper>(relaxed = true)
        val mockContext = mockk<Context>(relaxed = true)
        val magnesResult = mockk<MagnesResult>(relaxed = true)

        every { magnesResult.paypalClientMetaDataId } returns clientMetadataId
        every { mockUUIDHelper.getInstallationGUID(any()) } returns appGUID
        every { mockMagnesSDK.collectAndSubmit(any(), any(), any()) } returns magnesResult

        val sut = PayPalDataCollector(sandboxConfig, mockMagnesSDK, mockUUIDHelper)
        val request = PayPalDataCollectorRequest(
            hasUserLocationConsent = false,
            clientMetadataId = clientMetadataId
        )
        val result = sut.collectDeviceData(mockContext, request)
        assertEquals(result, clientMetadataId)
    }

    @Test
    fun `when setLogging is called, System is called with correct value`() {
        val sut =
            PayPalDataCollector(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        sut.setLogging(true)
        assertEquals(System.getProperty("magnes.debug.mode"), true.toString())
    }

    @Test
    fun `collectDeviceData forwards hasUserLocationConsent value`() {
        val appGUID = UUID.randomUUID().toString()
        val mockMagnesSDK = mockk<MagnesSDK>(relaxed = true)
        val mockUUIDHelper = mockk<UUIDHelper>(relaxed = true)
        val magnesSettingsSlot = slot<MagnesSettings>()

        every { mockUUIDHelper.getInstallationGUID(any()) } returns appGUID

        val sut = PayPalDataCollector(liveConfig, mockMagnesSDK, mockUUIDHelper)
        val request = PayPalDataCollectorRequest(hasUserLocationConsent = true)
        sut.collectDeviceData(context, request)

        verify { mockMagnesSDK.setUp(capture(magnesSettingsSlot)) }

        val magnesSettings = magnesSettingsSlot.captured
        assertTrue(magnesSettings.hasUserLocationConsent())
    }
}
