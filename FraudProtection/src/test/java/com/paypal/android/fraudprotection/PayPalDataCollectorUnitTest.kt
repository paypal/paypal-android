package com.paypal.android.fraudprotection

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import lib.android.paypal.com.magnessdk.Environment
import lib.android.paypal.com.magnessdk.InvalidInputException
import lib.android.paypal.com.magnessdk.MagnesResult
import lib.android.paypal.com.magnessdk.MagnesSDK
import lib.android.paypal.com.magnessdk.MagnesSettings
import lib.android.paypal.com.magnessdk.MagnesSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.UUID
import kotlin.collections.HashMap

class PayPalDataCollectorUnitTest {

    @Test
    fun `when getClientMetadataId is called, magnes settings has default values`() {
        val appGUID = UUID.randomUUID().toString()
        val mockMagnesSDK = mockk<MagnesSDK>(relaxed = true)
        val mockUUIDHelper = mockk<UUIDHelper>(relaxed = true)
        val magnesSettingsSlot = slot<MagnesSettings>()

        every { mockUUIDHelper.getInstallationGUID(any()) } returns appGUID
        val sut = PayPalDataCollector(PayPalDataCollectorEnvironment.SANDBOX, mockMagnesSDK, mockUUIDHelper)
        sut.getClientMetadataId(mockk(relaxed = true))
        verify { mockMagnesSDK.setUp(capture(magnesSettingsSlot)) }

        val magnesSettings = magnesSettingsSlot.captured
        assertEquals(magnesSettings.environment, Environment.SANDBOX)
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

        val sut = PayPalDataCollector(PayPalDataCollectorEnvironment.LIVE, mockMagnesSDK, mockUUIDHelper)
        sut.getClientMetadataId(mockk(relaxed = true))

        verify { mockMagnesSDK.setUp(capture(magnesSettingsSlot)) }

        val magnesSettings = magnesSettingsSlot.captured
        assertEquals(magnesSettings.environment, Environment.LIVE)
    }

    @Test
    fun `when appGUID is invalid, InvalidInputException is thrown`() {
        mockkStatic(Log::class)
        val errorMessage = "Application’s Globally Unique Identifier (AppGUID) does not match the criteria," +
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

        val sut = PayPalDataCollector(PayPalDataCollectorEnvironment.SANDBOX, mockMagnesSDK, mockUUIDHelper)
        val result = sut.getClientMetadataId(mockk(relaxed = true))

        verify { Log.e(any(), any(), capture(exceptionSlot)) }

        assertEquals(exceptionSlot.captured.message, errorMessage)
        assertEquals("", result)
    }

    @Test
    fun `when getClientMetadataId is called with correct values, it returns client metadata`() {
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

        val sut = PayPalDataCollector(PayPalDataCollectorEnvironment.SANDBOX, mockMagnesSDK, mockUUIDHelper)
        val result = sut.getClientMetadataId(mockContext, clientMetadataId, HashMap())
        assertEquals(result, clientMetadataId)
    }

    @Test
    fun `when setLogging is called, System is called with correct value`() {
        mockkStatic(System::class)
        val sut = PayPalDataCollector(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        sut.setLogging(true)

        verify { System.setProperty("magnes.debug.mode", true.toString()) }
    }
}
