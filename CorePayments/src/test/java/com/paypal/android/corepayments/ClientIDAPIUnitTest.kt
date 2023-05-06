package com.paypal.android.corepayments

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ClientIDAPIUnitTest {

    private val http = mockk<Http>(relaxed = true)
    private val httpRequestFactory = mockk<HttpRequestFactory>()

    private val apiRequest = APIRequest("/sample/path", HttpMethod.GET, null)
    private val configuration = CoreConfig("fake-access-token")

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

    private lateinit var sut: ClientIDAPI

    @Before
    fun beforeEach() {
        sut = ClientIDAPI(configuration, http, httpRequestFactory)
        ClientIDAPI.clientIDCache.evictAll()
    }

    // COPIED FROM API_UNIT_TESTS

    @Test
    fun `fetchCachedOrRemoteClientID() sends oauth api request when value not in cache`() = runTest {
        val apiRequestSlot = slot<APIRequest>()
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(
                capture(apiRequestSlot),
                configuration
            )
        } returns httpRequest

        coEvery { http.send(httpRequest) } returns clientIdSuccessResponse

        sut.fetchCachedOrRemoteClientID()

        val apiRequest = apiRequestSlot.captured
        assertEquals(HttpMethod.GET, apiRequest.method)
        assertEquals("v1/oauth2/token", apiRequest.path)
    }

    @Test
    fun `fetchCachedOrRemoteClientID() puts value in cache after fetched`() = runTest {
        val apiRequestSlot = slot<APIRequest>()
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(
                capture(apiRequestSlot),
                configuration
            )
        } returns httpRequest

        coEvery { http.send(httpRequest) } returns clientIdSuccessResponse

        sut.fetchCachedOrRemoteClientID()

        assertEquals(ClientIDAPI.clientIDCache.get("fake-access-token"), "sample-client-id")
    }

    @Test
    fun `fetchCachedOrRemoteClientID() returns cached value when exists in cache`() = runTest {
        ClientIDAPI.clientIDCache.put("fake-access-token", "cached-id-123")

        val clientID = sut.fetchCachedOrRemoteClientID()
        assertEquals(clientID, "cached-id-123")
    }

    @Test
    fun `fetchCachedOrRemoteClientID() returns client id from JSON`() = runTest {
        every {
            httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
        } returns httpRequest

        coEvery { http.send(httpRequest) } returns clientIdSuccessResponse

        val result = sut.fetchCachedOrRemoteClientID()
        assertEquals("sample-client-id", result)
    }

    @Test
    fun `fetchCachedOrRemoteClientID() throws no response data error when http response has no body`() =
        runTest {

            every {
                httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
            } returns httpRequest

            val noBodyHttpResponse = HttpResponse(200, httpResponseHeaders)
            coEvery { http.send(httpRequest) } returns noBodyHttpResponse

            var capturedError: PayPalSDKError? = null
            try {
                sut.fetchCachedOrRemoteClientID()
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(Code.NO_RESPONSE_DATA.ordinal, capturedError?.code)
            assertEquals("sample-correlation-id", capturedError?.correlationID)
        }

    @Test
    fun `fetchCachedOrRemoteClientID() throws data parsing error when http response is missing client id`() =
        runTest {

            every {
                httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
            } returns httpRequest

            val httpResponseWithoutClientId =
                HttpResponse(200, httpResponseHeaders, "{}")
            coEvery { http.send(httpRequest) } returns httpResponseWithoutClientId

            var capturedError: PayPalSDKError? = null
            try {
                sut.fetchCachedOrRemoteClientID()
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(Code.DATA_PARSING_ERROR.ordinal, capturedError?.code)
            assertEquals("sample-correlation-id", capturedError?.correlationID)
        }

    @Test
    fun `fetchCachedOrRemoteClientID() throws server response error when http response is unsuccessful`() =
        runTest {

            every {
                httpRequestFactory.createHttpRequestFromAPIRequest(any(), any())
            } returns httpRequest

            val httpResponseWithoutClientId = HttpResponse(500, httpResponseHeaders)
            coEvery { http.send(httpRequest) } returns httpResponseWithoutClientId

            var capturedError: PayPalSDKError? = null
            try {
                sut.fetchCachedOrRemoteClientID()
            } catch (e: PayPalSDKError) {
                capturedError = e
            }
            assertEquals(Code.SERVER_RESPONSE_ERROR.ordinal, capturedError?.code)
            assertEquals("sample-correlation-id", capturedError?.correlationID)
        }
}