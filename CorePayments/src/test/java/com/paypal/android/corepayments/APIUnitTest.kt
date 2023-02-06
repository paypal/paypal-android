package com.paypal.android.corepayments

import com.paypal.android.corepayments.analytics.AnalyticsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
    private val analyticsService = mockk<AnalyticsService>()

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

    private lateinit var sut: API

    @Before
    fun beforeEach() {
        sut = API(configuration, http, httpRequestFactory, analyticsService)
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
    fun `send analytics event delegates it to analytics client`() = runTest {
        coEvery {
            analyticsService.sendAnalyticsEvent(
                "sample.event.name",
                "fake-client-id"
            )
        } just runs
        sut.sendAnalyticsEvent("sample.event.name")
        coVerify(exactly = 1) {
            analyticsService.sendAnalyticsEvent("sample.event.name", "fake-client-id")
        }
    }
}
