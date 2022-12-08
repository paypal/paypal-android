package com.paypal.android.core.analytics

import com.paypal.android.core.Http
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.HttpRequest
import com.paypal.android.core.HttpRequestFactory
import com.paypal.android.core.HttpResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class AnalyticsServiceTest {

    private lateinit var http: Http
    private lateinit var httpRequestFactory: HttpRequestFactory
    private lateinit var analyticsService: AnalyticsService

    lateinit var deviceInspector: DeviceInspector

    private val url = URL("https://example.com/resolved/path")
    private val httpRequest = HttpRequest(url, HttpMethod.GET)

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
        http = mockk()
        httpRequestFactory = mockk()
        deviceInspector = mockk()
        every { deviceInspector.inspect() } returns deviceData
        analyticsService = AnalyticsService(deviceInspector, http, httpRequestFactory)

        coEvery { http.send(httpRequest) } returns HttpResponse(200)
    }

    @Test
    fun `sendAnalyticsEvent send proper AnalyticsEventData`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        analyticsService.sendAnalyticsEvent("sample.event.name")

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sample.event.name", analyticsEventData.eventName)
        assertSame(deviceData, analyticsEventData.deviceData)
    }

    @Test
    fun `sendAnalyticsEvent calls HTTP send`() = runTest {
        every {
            httpRequestFactory.createHttpRequestForAnalytics(any())
        } returns httpRequest

        analyticsService.sendAnalyticsEvent("sample.event.name")

        coVerify(exactly = 1) {
            http.send(httpRequest)
        }
    }

    fun `sendAnalyticsEvent sends proper timestamp`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        val timeBeforeEventSent = System.currentTimeMillis()
        analyticsService.sendAnalyticsEvent("sample.event.name")

        val actualTimestamp = analyticsEventDataSlot.captured.timestamp

        assert(actualTimestamp > timeBeforeEventSent)
        assert(actualTimestamp < System.currentTimeMillis())
    }

    @Test
    fun `analyticsClient uses singleton for sessionId`() = runTest {
        val analyticsEventDataSlot1 = slot<AnalyticsEventData>()

        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot1))
        } returns httpRequest

        analyticsService.sendAnalyticsEvent("event1")
        val analyticsEventData1 = analyticsEventDataSlot1.captured

        val analyticsEventDataSlot2 = slot<AnalyticsEventData>()
        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot2))
        } returns httpRequest

        val analyticsService2 = AnalyticsService(deviceInspector, http, httpRequestFactory)
        analyticsService2.sendAnalyticsEvent("event2")

        val analyticsEventData2 = analyticsEventDataSlot2.captured

        assertEquals(analyticsEventData1.sessionID, analyticsEventData2.sessionID)
    }
}
