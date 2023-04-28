package com.paypal.android.corepayments.analytics

import com.paypal.android.corepayments.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class AnalyticsServiceTest {

    private lateinit var sut: AnalyticsService
    private lateinit var environment: Environment

    private lateinit var trackingEventsAPI: TrackingEventsAPI
    private lateinit var secureTokenServiceAPI: SecureTokenServiceAPI
    private lateinit var deviceInspector: DeviceInspector

    private val httpSuccessResponse = HttpResponse(200)

    private val deviceData = DeviceData(
        appName = "app name",
        appId = "app id",
        clientSDKVersion = "1.2.3",
        clientOS = "123",
        deviceManufacturer = "device manufacturer",
        deviceModel = "device model",
        isSimulator = false,
        merchantAppVersion = "4.5.6"
    )

    @Before
    fun setup() {
        deviceInspector = mockk()
        trackingEventsAPI = mockk(relaxed = true)
        secureTokenServiceAPI = mockk(relaxed = true)
        environment = Environment.SANDBOX

        every { deviceInspector.inspect() } returns deviceData
        coEvery { secureTokenServiceAPI.fetchCachedOrRemoteClientID() } returns "fake-client-id"
        sut =
            AnalyticsService(deviceInspector, environment, trackingEventsAPI, secureTokenServiceAPI)
    }

    @Test
    fun `sendAnalyticsEvent send proper AnalyticsEventData`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot), deviceData)
        } returns httpSuccessResponse

        sut.sendAnalyticsEvent("sample.event.name")

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sample.event.name", analyticsEventData.eventName)
    }

    fun `sendAnalyticsEvent sends proper timestamp`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot), deviceData)
        } returns httpSuccessResponse

        val timeBeforeEventSent = System.currentTimeMillis()
        sut.sendAnalyticsEvent("sample.event.name")

        val actualTimestamp = analyticsEventDataSlot.captured.timestamp
        assert(actualTimestamp > timeBeforeEventSent)
        assert(actualTimestamp < System.currentTimeMillis())
    }

    @Test
    fun `sendAnalyticsEvent sends proper tag when SANDBOX`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot), deviceData)
        } returns httpSuccessResponse

        sut.sendAnalyticsEvent("fake-event")

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sandbox", analyticsEventData.environment)
    }

    @Test
    fun `sendAnalyticsEvent sends proper tag when LIVE`() = runTest {
        sut = AnalyticsService(
            deviceInspector,
            Environment.LIVE,
            trackingEventsAPI,
            secureTokenServiceAPI
        )

        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot), deviceData)
        } returns httpSuccessResponse

        sut.sendAnalyticsEvent("fake-event")

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("live", analyticsEventData.environment)
    }

    @Test
    fun `analyticsClient uses singleton for sessionId`() = runTest {
        val analyticsEventDataSlot1 = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot1), deviceData)
        } returns httpSuccessResponse

        sut.sendAnalyticsEvent("event1")
        val analyticsEventData1 = analyticsEventDataSlot1.captured

        val analyticsEventDataSlot2 = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot2), deviceData)
        } returns httpSuccessResponse

        val sut2 =
            AnalyticsService(deviceInspector, environment, trackingEventsAPI, secureTokenServiceAPI)
        sut2.sendAnalyticsEvent("event2")

        val analyticsEventData2 = analyticsEventDataSlot2.captured
        assertEquals(analyticsEventData1.sessionID, analyticsEventData2.sessionID)
    }
}
