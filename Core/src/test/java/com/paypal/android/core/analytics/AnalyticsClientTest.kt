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
import io.mockk.spyk
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
class AnalyticsClientTest {

    private lateinit var http: Http

    private lateinit var httpRequestFactory: HttpRequestFactory

    private lateinit var analyticsClient: AnalyticsClient

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
        analyticsClient = AnalyticsClient(deviceInspector, http, httpRequestFactory)
    }

    @Test
    fun `sendAnalyticsEvent internal`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest
        val httpResponse = HttpResponse(200)
        coEvery { http.send(httpRequest) } returns httpResponse

        analyticsClient.sendAnalyticsEvent("sample.event.name", 789)

        val analyticsEventData1 = analyticsEventDataSlot.captured
        assertEquals("sample.event.name", analyticsEventData1.eventName)
        assertEquals(789, analyticsEventData1.timestamp)
        assertSame(deviceData, analyticsEventData1.deviceData)
        val firstSessionId = analyticsEventData1.sessionID
        analyticsClient.sendAnalyticsEvent("second event", 200L)
        val analyticsEventData2 = analyticsEventDataSlot.captured
        val secondSessionId = analyticsEventData2.sessionID
        assertEquals("second event", analyticsEventData2.eventName)
        assertEquals(firstSessionId, secondSessionId)
    }

    @Test
    fun `send analytics event verify time stamp `() = runTest {
        val timeStampSlot = slot<Long>()
        val currTimeStamp = System.currentTimeMillis()
        coEvery { http.send(any()) } returns mockk(relaxed = true)
        coEvery { httpRequestFactory.createHttpRequestForAnalytics(any()) } returns mockk(relaxed = true)
        val spyAnalyticsClient = spyk(analyticsClient)
        spyAnalyticsClient.sendAnalyticsEvent("sample.event.name")
        coVerify(exactly = 1) {
            spyAnalyticsClient.sendAnalyticsEvent("sample.event.name", capture(timeStampSlot))
        }
        assert(timeStampSlot.captured > currTimeStamp)
        assert(timeStampSlot.captured < System.currentTimeMillis())
    }
}
