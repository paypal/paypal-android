package com.paypal.android.corepayments.analytics

import com.paypal.android.corepayments.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
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
    }

    @Test
    fun `sendAnalyticsEvent send proper AnalyticsEventData`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot), deviceData)
        } returns httpSuccessResponse

        sut = createAnalyticsService(environment, testScheduler)
        sut.sendAnalyticsEvent("sample.event.name", "fake-order-id")
        advanceUntilIdle()

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sample.event.name", analyticsEventData.eventName)
    }

    fun `sendAnalyticsEvent sends proper timestamp`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot), deviceData)
        } returns httpSuccessResponse

        val timeBeforeEventSent = System.currentTimeMillis()
        sut = createAnalyticsService(environment, testScheduler)
        sut.sendAnalyticsEvent("sample.event.name", "fake-order-id")
        advanceUntilIdle()

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

        sut = createAnalyticsService(environment, testScheduler)
        sut.sendAnalyticsEvent("fake-event", "fake-order-id")
        advanceUntilIdle()

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sandbox", analyticsEventData.environment)
    }

    @Test
    fun `sendAnalyticsEvent sends proper tag when LIVE`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot), deviceData)
        } returns httpSuccessResponse

        sut = createAnalyticsService(Environment.LIVE, testScheduler)
        sut.sendAnalyticsEvent("fake-event", "fake-order-id")
        advanceUntilIdle()

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("live", analyticsEventData.environment)
    }

    @Test
    fun `analyticsClient uses singleton for sessionId`() = runTest {
        val analyticsEventDataSlot1 = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot1), deviceData)
        } returns httpSuccessResponse

        sut = createAnalyticsService(environment, testScheduler)
        sut.sendAnalyticsEvent("event1", "fake-order-id")
        advanceUntilIdle()
        val analyticsEventData1 = analyticsEventDataSlot1.captured

        val analyticsEventDataSlot2 = slot<AnalyticsEventData>()
        coEvery {
            trackingEventsAPI.sendEvent(capture(analyticsEventDataSlot2), deviceData)
        } returns httpSuccessResponse

        val sut2 = createAnalyticsService(environment, testScheduler)
        sut2.sendAnalyticsEvent("event2", "fake-order-id")
        advanceUntilIdle()

        val analyticsEventData2 = analyticsEventDataSlot2.captured
        assertEquals(analyticsEventData1.sessionID, analyticsEventData2.sessionID)
    }

    private fun createAnalyticsService(
        environment: Environment,
        testScheduler: TestCoroutineScheduler
    ): AnalyticsService {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)

        // Ref: https://developer.android.com/kotlin/coroutines/test#injecting-test-dispatchers
        return AnalyticsService(
            deviceInspector,
            environment,
            trackingEventsAPI,
            secureTokenServiceAPI,
            scope
        )
    }
}
