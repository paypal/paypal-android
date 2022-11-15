package com.paypal.android.core

import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
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

    private val http = mockk<Http>()
    private val httpRequestFactory = mockk<HttpRequestFactory>()

    private val apiRequest = APIRequest("/sample/path", HttpMethod.GET, null)
    private val configuration = CoreConfig()

    private val httpResponseHeaders = mapOf(
        "Paypal-Debug-Id" to "sample-correlation-id"
    )

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private val clientIdSuccessResponse by lazy {
        val clientIdBody = JSONObject()
            .put("client_id", "sample-client-id")
            .toString()
        HttpResponse(200, httpResponseHeaders, clientIdBody)
    }

    private lateinit var sut: API

    @Before
    fun beforeEach() {
        sut = API(configuration, http, httpRequestFactory, null)

        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun afterEach() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `converts an api request to an http request and sends it`() = runBlocking {
        val url = URL("https://example.com/resolved/path")

        val httpRequest = HttpRequest(url, HttpMethod.GET)
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(apiRequest, configuration)
        } returns httpRequest

        val httpResponse = HttpResponse(200)
        coEvery { http.send(httpRequest) } returns httpResponse

        val result = sut.send(apiRequest)
        assertSame(httpResponse, result)
    }

    @Test
    fun `get client id sends oauth api request`() = runBlocking {
        val url = URL("https://example.com/resolved/path")
        val httpRequest = HttpRequest(url, HttpMethod.GET)

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
    fun `get client id returns client id from JSON`() = runBlocking {
        val url = URL("https://example.com/resolved/path")
        val httpRequest = HttpRequest(url, HttpMethod.GET)

        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
        } returns httpRequest

        coEvery { http.send(httpRequest) } returns clientIdSuccessResponse

        val result = sut.getClientId()
        assertEquals("sample-client-id", result)
    }

    @Test
    fun `get client id throws no response data error when http response has no body`() =
        runBlocking {
            val url = URL("https://example.com/resolved/path")
            val httpRequest = HttpRequest(url, HttpMethod.GET)

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
        runBlocking {
            val url = URL("https://example.com/resolved/path")
            val httpRequest = HttpRequest(url, HttpMethod.GET)

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
        runBlocking {
            val url = URL("https://example.com/resolved/path")
            val httpRequest = HttpRequest(url, HttpMethod.GET)

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
}
