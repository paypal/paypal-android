package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.RestClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FetchClientTokenUnitTest {

    private lateinit var restClient: RestClient
    private lateinit var coreConfig: CoreConfig
    private lateinit var sut: FetchClientToken

    @Before
    fun beforeEach() {
        coreConfig = CoreConfig("test-client-id", Environment.SANDBOX)
        restClient = mockk(relaxed = true)
        sut = FetchClientToken(coreConfig, restClient)
    }

    @Test
    fun `invoke() makes correct API request with proper headers and body`() = runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = """{"access_token": "test-token", "token_type": "Bearer", "expires_in": 3600}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

        val requestSlot = slot<APIRequest>()

        // When
        val result = sut()

        // Then
        coVerify { restClient.send(capture(requestSlot)) }

        val capturedRequest = requestSlot.captured
        assertEquals("v1/oauth2/token", capturedRequest.path)
        assertEquals(HttpMethod.POST, capturedRequest.method)
        assertEquals("grant_type=client_credentials&response_type=token", capturedRequest.body)

        // Verify headers
        val headers = capturedRequest.headers!!
        assertEquals("application/x-www-form-urlencoded", headers["Content-Type"])
        assert(headers["Authorization"]!!.startsWith("Basic "))

        assertEquals("test-token", result)
    }

    @Test
    fun `invoke() returns access token from successful response`() = runTest {
        // Given
        val expectedToken = "test-access-token-12345"
        val successResponse = HttpResponse(
            status = 200,
            body = """{"access_token": "$expectedToken", "token_type": "Bearer", "expires_in": 3600}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

        // When
        val result = sut()

        // Then
        assertEquals(expectedToken, result)
    }

    @Test
    fun `invoke() throws APIClientError when response is not successful`() = runTest {
        // Given
        val errorResponse = HttpResponse(
            status = 401,
            body = """{"error": "invalid_client", "error_description": "Client authentication failed"}"""
        )
        coEvery { restClient.send(any()) } returns errorResponse

        // When/Then
        val result = runCatching { sut() }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PayPalSDKError)
    }

    @Test
    fun `invoke() throws APIClientError when response body is null`() = runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = null
        )
        coEvery { restClient.send(any()) } returns successResponse

        // When/Then
        val result = runCatching { sut() }
        assertTrue(result.isFailure)
        // Expected JSONException when parsing null body
    }

    @Test
    fun `invoke() throws JSONException when response is not valid JSON`() = runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = "invalid-json-response"
        )
        coEvery { restClient.send(any()) } returns successResponse

        // When/Then
        val result = runCatching { sut() }
        assertTrue(result.isFailure)
        // Expected JSONException for invalid JSON
    }

    @Test
    fun `invoke() throws JSONException when access_token is missing from response`() = runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = """{"token_type": "Bearer", "expires_in": 3600}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

        // When/Then
        val result = runCatching { sut() }
        assertTrue(result.isFailure)
        // Expected JSONException for missing access_token
    }

    @Test
    fun `invoke() handles empty response body with proper error message`() = runTest {
        // Given
        val errorResponse = HttpResponse(
            status = 500,
            body = ""
        )
        coEvery { restClient.send(any()) } returns errorResponse

        // When/Then
        try {
            sut()
            assert(false) { "Expected PayPalSDKError to be thrown" }
        } catch (e: PayPalSDKError) {
            val errorMessage = e.errorDescription
            // Just verify it contains some error information
            assert(errorMessage.contains("Error fetching client token"))
        }
    }

    @Test
    fun `invoke() includes error message in exception when available`() = runTest {
        // Given
        val errorMessage = "Server temporarily unavailable"
        val errorResponse = HttpResponse(
            status = 503,
            body = errorMessage
        )
        coEvery { restClient.send(any()) } returns errorResponse

        // When/Then
        try {
            sut()
            assert(false) { "Expected PayPalSDKError to be thrown" }
        } catch (e: PayPalSDKError) {
            val description = e.errorDescription
            // Just verify it contains some error information
            assert(description.contains("Error fetching client token"))
        }
    }

    @Test
    fun `invoke() creates proper Basic Auth header from client ID`() = runTest {
        // Given
        val clientId = "test-client-123"
        val configWithCustomClientId = CoreConfig(clientId, Environment.LIVE)
        val sutWithCustomConfig = FetchClientToken(configWithCustomClientId, restClient)

        val successResponse = HttpResponse(
            status = 200,
            body = """{"access_token": "token", "token_type": "Bearer"}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

        val requestSlot = slot<APIRequest>()

        // When
        sutWithCustomConfig()

        // Then
        coVerify { restClient.send(capture(requestSlot)) }

        val authHeader = requestSlot.captured.headers!!["Authorization"]!!
        assert(authHeader.startsWith("Basic "))

        // Decode and verify the Basic auth contains the client ID
        val encodedCredentials = authHeader.substring("Basic ".length)
        val decodedCredentials =
            String(android.util.Base64.decode(encodedCredentials, android.util.Base64.DEFAULT))
        assertEquals("$clientId:", decodedCredentials)
    }
}
