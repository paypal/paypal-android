package com.paypal.android.corepayments.analytics

import com.paypal.android.corepayments.*
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
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

    private lateinit var sut: AnalyticsService

    @Before
    fun setup() {
        http = mockk()
        httpRequestFactory = mockk()
        clientIDAPI = mockk()
        deviceInspector = mockk()
        environment = Environment.SANDBOX

        every { deviceInspector.inspect() } returns deviceData
        coEvery { clientIDAPI.fetchCachedOrRemoteClientID() } returns "fake-client-id"
        coEvery { http.send(httpRequest) } returns HttpResponse(200)
    }

    @Test
    fun `sendAnalyticsEvent send proper AnalyticsEventData`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        sut = getAnalyticsService(testScheduler = testScheduler)

        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        sut.sendAnalyticsEvent("sample.event.name")
        advanceUntilIdle()

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sample.event.name", analyticsEventData.eventName)
        assertSame(deviceData, analyticsEventData.deviceData)
    }

    @Test
    fun `sendAnalyticsEvent calls HTTP send`() = runTest {
        sut = getAnalyticsService(testScheduler)

        every {
            httpRequestFactory.createHttpRequestForAnalytics(any())
        } returns httpRequest

        sut.sendAnalyticsEvent("sample.event.name")
        advanceUntilIdle()

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
        sut.sendAnalyticsEvent("sample.event.name")

        val actualTimestamp = analyticsEventDataSlot.captured.timestamp

        assert(actualTimestamp > timeBeforeEventSent)
        assert(actualTimestamp < System.currentTimeMillis())
    }

    @Test
    fun `sendAnalyticsEvent sends proper tag when SANDBOX`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        sut = getAnalyticsService(testScheduler)

        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        sut.sendAnalyticsEvent("fake-event")
        advanceUntilIdle()

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sandbox", analyticsEventData.environment)
    }

    @Test
    fun `sendAnalyticsEvent sends proper tag when LIVE`() = runTest {
        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        sut = getAnalyticsService(testScheduler, Environment.LIVE)

        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        sut.sendAnalyticsEvent("fake-event")
        advanceUntilIdle()

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("live", analyticsEventData.environment)
    }

    @Test
    fun `sendAnalyticEvent does not send analytics via HTTP if clientID fetch fails`() = runTest {
        val error = PayPalSDKError(0, "client_id_error")
        coEvery { clientIDAPI.fetchCachedOrRemoteClientID() } throws error

        sut = getAnalyticsService(testScheduler)

        sut.sendAnalyticsEvent("sample.event.name")
        advanceUntilIdle()

        verify(exactly = 0) { httpRequestFactory.createHttpRequestForAnalytics(any()) }
    }

    private fun getAnalyticsService(
        testScheduler: TestCoroutineScheduler? = null,
        environment: Environment = Environment.SANDBOX
    ): AnalyticsService {
        return testScheduler?.let {
            val dispatcher = StandardTestDispatcher(testScheduler)
            AnalyticsService(
                deviceInspector,
                clientIDAPI,
                environment,
                http,
                httpRequestFactory,
                "fake-order-id",
                dispatcher
            )
        } ?: AnalyticsService(
            deviceInspector,
            clientIDAPI,
            environment,
            http,
            httpRequestFactory,
            "fake-order-id"
        )
    }
}
