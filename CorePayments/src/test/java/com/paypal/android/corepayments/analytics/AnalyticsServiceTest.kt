package com.paypal.android.corepayments.analytics

import com.paypal.android.corepayments.*
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
    private lateinit var environment: Environment
    private lateinit var clientIDAPI: ClientIDAPI
    private lateinit var deviceInspector: DeviceInspector

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
        clientIDAPI = mockk()
        deviceInspector = mockk()
        environment = Environment.SANDBOX

        every { deviceInspector.inspect() } returns deviceData
        analyticsService = AnalyticsService(
            deviceInspector,
            clientIDAPI,
            environment,
            http,
            httpRequestFactory,
            "fake-order-id"
        )

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
    fun `sendAnalyticsEvent sends proper tag when SANDBOX`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        analyticsService.sendAnalyticsEvent("fake-event")

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sandbox", analyticsEventData.environment)
    }

    @Test
    fun `sendAnalyticsEvent sends proper tag when LIVE`() = runTest {
        analyticsService = AnalyticsService(
            deviceInspector,
            clientIDAPI,
            Environment.LIVE,
            http,
            httpRequestFactory,
            "fake-order-id"
        )

        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        analyticsService.sendAnalyticsEvent("fake-event")

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("live", analyticsEventData.environment)
    }
}
