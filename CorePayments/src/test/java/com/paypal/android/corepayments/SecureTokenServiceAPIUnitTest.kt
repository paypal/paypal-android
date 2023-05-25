package com.paypal.android.corepayments

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SecureTokenServiceAPIUnitTest {

    private val restClient = mockk<RestClient>()
    private val httpResponseHeaders = mapOf(
        "Paypal-Debug-Id" to "sample-correlation-id"
    )

    private val clientIdSuccessResponse by lazy {
        val clientIdBody = JSONObject()
            .put("client_id", "sample-client-id")
            .toString()
        HttpResponse(200, httpResponseHeaders, clientIdBody)
    }

    private lateinit var sut: SecureTokenServiceAPI

    @Before
    fun beforeEach() {
        sut = SecureTokenServiceAPI(restClient)
    }

    @Test
    fun `getClientId() sends oauth api request when value not in cache`() =
        runTest {
            val apiRequestSlot = slot<APIRequest>()
            coEvery { restClient.send(capture(apiRequestSlot)) } returns clientIdSuccessResponse

            sut.getClientId()

            val apiRequest = apiRequestSlot.captured
            assertEquals(HttpMethod.GET, apiRequest.method)
            assertEquals("v1/oauth2/token", apiRequest.path)
        }

    @Test
    fun `getClientId() returns client id from JSON`() = runTest {
        coEvery { restClient.send(any()) } returns clientIdSuccessResponse

        val result = sut.getClientId()
        assertEquals("sample-client-id", result)
    }

    @Test
    fun `getClientId() throws no response data error when http response has no body`() = runTest {
        val noBodyHttpResponse = HttpResponse(200, httpResponseHeaders)
        coEvery { restClient.send(any()) } returns noBodyHttpResponse

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
    fun `getClientId() throws data parsing error when http response is missing client id`() =
        runTest {
            val missingClientIdResponse = HttpResponse(200, httpResponseHeaders, "{}")
            coEvery { restClient.send(any()) } returns missingClientIdResponse

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
    fun `getClientId() throws server response error when http response is unsuccessful`() =
        runTest {
            val failedServerResponse = HttpResponse(500, httpResponseHeaders)
            coEvery { restClient.send(any()) } returns failedServerResponse

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
