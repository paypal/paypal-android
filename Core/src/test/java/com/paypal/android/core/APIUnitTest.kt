package com.paypal.android.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.core.analytics.AnalyticsEventData
import com.paypal.android.core.analytics.DeviceData
import com.paypal.android.core.analytics.DeviceInspector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class APIUnitTest {
    private val http = mockk<Http>(relaxed = true)
    private val httpRequestFactory = mockk<HttpRequestFactory>()

    private val deviceInspector = mockk<DeviceInspector>()

    private val apiRequest = APIRequest("/sample/path", HttpMethod.GET, null)
    private val configuration = CoreConfig()

    private val httpResponseHeaders = mapOf(
        "Paypal-Debug-Id" to "sample-correlation-id"
    )

    private val url = URL("https://example.com/resolved/path")
    private val httpRequest = HttpRequest(url, HttpMethod.GET)

    private val clientIdSuccessResponse by lazy {
        val clientIdBody = JSONObject()
            .put("client_id", "sample-client-id")
            .toString()
        HttpResponse(200, httpResponseHeaders, clientIdBody)
    }

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

    private lateinit var context: Context
    private lateinit var sut: API

    @Before
    fun beforeEach() {
        context = ApplicationProvider.getApplicationContext()
        sut = API(configuration, "session-id", http, httpRequestFactory, deviceInspector)
    }

    @Test
    fun `converts an api request to an http request and sends it`() = runTest {
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        } returns httpRequest

        val httpResponse = HttpResponse(200)
        coEvery { http.send(httpRequest) } returns httpResponse

        val result = sut.send(apiRequest)
        assertSame(httpResponse, result)
    }

    @Test
    fun `get client id sends oauth api request`() = runTest {
        val apiRequestSlot = slot<APIRequest>()
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(
                capture(apiRequestSlot),
                configuration
            )
        } returns httpRequest

        coEvery { http.send(httpRequest) } returns clientIdSuccessResponse

        sut.getClientId()

        val apiRequest = apiRequestSlot.captured
        assertEquals(HttpMethod.GET, apiRequest.method)
        assertEquals("v1/oauth2/token", apiRequest.path)
    }

    @Test
    fun `get client id returns client id from JSON`() = runTest {
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
        } returns httpRequest

        coEvery { http.send(httpRequest) } returns clientIdSuccessResponse

        val result = sut.getClientId()
        assertEquals("sample-client-id", result)
    }

    @Test
    fun `get client id throws no response data error when http response has no body`() =
        runTest {

            every {
                httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
            } returns httpRequest

            val noBodyHttpResponse = HttpResponse(200, httpResponseHeaders)
            coEvery { http.send(httpRequest) } returns noBodyHttpResponse

            var capturedError: PayPalSDKError? = null
            try {
                sut.getClientId()
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(Code.NO_RESPONSE_DATA.ordinal, capturedError?.code)
            assertEquals("sample-correlation-id", capturedError?.correlationID)
        }

    @Test
    fun `get client id throws data parsing error when http response is missing client id`() =
        runTest {

            every {
                httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
            } returns httpRequest

            val httpResponseWithoutClientId =
                HttpResponse(200, httpResponseHeaders, "{}")
            coEvery { http.send(httpRequest) } returns httpResponseWithoutClientId

            var capturedError: PayPalSDKError? = null
            try {
                sut.getClientId()
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(Code.DATA_PARSING_ERROR.ordinal, capturedError?.code)
            assertEquals("sample-correlation-id", capturedError?.correlationID)
        }

    @Test
    fun `get client id throws server response error when http response is unsuccessful`() =
        runTest {

            every {
                httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
            } returns httpRequest

            val httpResponseWithoutClientId = HttpResponse(500, httpResponseHeaders)
            coEvery { http.send(httpRequest) } returns httpResponseWithoutClientId

            var capturedError: PayPalSDKError? = null
            try {
                sut.getClientId()
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(Code.SERVER_RESPONSE_ERROR.ordinal, capturedError?.code)
            assertEquals("sample-correlation-id", capturedError?.correlationID)
        }

    @Test
    fun `send analytics event creates an http request via http request factory`() = runTest {
        every { deviceInspector.inspect() } returns deviceData

        val analyticsEventDataSlot = slot<AnalyticsEventData>()
        every {
            httpRequestFactory.createHttpRequestForAnalytics(capture(analyticsEventDataSlot))
        } returns httpRequest

        sut.sendAnalyticsEvent("sample.event.name", 789)

        val analyticsEventData = analyticsEventDataSlot.captured
        assertEquals("sample.event.name", analyticsEventData.eventName)
        assertEquals(789, analyticsEventData.timestamp)
        assertEquals("session-id", analyticsEventData.sessionID)
        assertSame(deviceData, analyticsEventData.deviceData)
    }

    @Test
    fun `send analytics event sends an http request created from an analytics event`() = runTest {
        every { deviceInspector.inspect() } returns deviceData
        every { httpRequestFactory.createHttpRequestForAnalytics(any()) } returns httpRequest

        sut.sendAnalyticsEvent("sample.event.name", 789)
        coVerify { http.send(httpRequest) }
    }
}
