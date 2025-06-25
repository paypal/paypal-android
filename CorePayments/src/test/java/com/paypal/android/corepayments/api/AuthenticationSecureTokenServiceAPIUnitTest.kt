package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.RestClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AuthenticationSecureTokenServiceAPIUnitTest {

    private lateinit var restClient: RestClient
    private lateinit var coreConfig: CoreConfig
    private lateinit var sut: AuthenticationSecureTokenServiceAPI

    @Before
    fun beforeEach() {
        coreConfig = CoreConfig("test-client-id", Environment.SANDBOX)
        restClient = mockk(relaxed = true)
        sut = AuthenticationSecureTokenServiceAPI(coreConfig, restClient)
    }

    @Test
    fun `createLowScopedAccessToken() makes correct API request with proper headers and body`() =
        runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = """{"access_token": "test-token", "token_type": "Bearer", "expires_in": 3600}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

        val requestSlot = slot<APIRequest>()

        // When
            val result = sut.createLowScopedAccessToken()

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

            assertTrue(result is CreateLowScopedAccessTokenResult.Success)
            assertEquals("test-token", (result as CreateLowScopedAccessTokenResult.Success).token)
    }

    @Test
    fun `createLowScopedAccessToken() returns access token from successful response`() = runTest {
        // Given
        val expectedToken = "test-access-token-12345"
        val successResponse = HttpResponse(
            status = 200,
            body = """{"access_token": "$expectedToken", "token_type": "Bearer", "expires_in": 3600}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

        // When
        val result = sut.createLowScopedAccessToken()

        // Then
        assertTrue(result is CreateLowScopedAccessTokenResult.Success)
        assertEquals(expectedToken, (result as CreateLowScopedAccessTokenResult.Success).token)
    }

    @Test
    fun `createLowScopedAccessToken() returns failure when response is not successful`() = runTest {
        // Given
        val errorResponse = HttpResponse(
            status = 401,
            body = """{"error": "invalid_client", "error_description": "Client authentication failed"}"""
        )
        coEvery { restClient.send(any()) } returns errorResponse

        // When
        val result = sut.createLowScopedAccessToken()

        // Then
        assertTrue(result is CreateLowScopedAccessTokenResult.Failure)
    }

    @Test
    fun `createLowScopedAccessToken() returns failure when response body is null`() = runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = null
        )
        coEvery { restClient.send(any()) } returns successResponse

        // When
        val result = sut.createLowScopedAccessToken()

        // Then
        assertTrue(result is CreateLowScopedAccessTokenResult.Failure)
    }

    @Test
    fun `createLowScopedAccessToken() returns failure when response is not valid JSON`() = runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = "invalid-json-response"
        )
        coEvery { restClient.send(any()) } returns successResponse

        // When
        val result = sut.createLowScopedAccessToken()

        // Then
        assertTrue(result is CreateLowScopedAccessTokenResult.Failure)
        // Expected JSONException wrapped in PayPalSDKError
    }

    @Test
    fun `createLowScopedAccessToken() returns failure when access_token field is missing from successful response`() =
        runTest {
        // Given
        val successResponse = HttpResponse(
            status = 200,
            body = """{"token_type": "Bearer", "expires_in": 3600}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

            // When
            val result = sut.createLowScopedAccessToken()

            // Then
            assertTrue(result is CreateLowScopedAccessTokenResult.Failure)
            val error = (result as CreateLowScopedAccessTokenResult.Failure).error
            assertTrue(error.errorDescription.contains("Missing access_token in response"))
    }

    @Test
    fun `createLowScopedAccessToken() handles empty response body with proper error message`() =
        runTest {
        // Given
        val errorResponse = HttpResponse(
            status = 500,
            body = ""
        )
        coEvery { restClient.send(any()) } returns errorResponse

        // When
            val result = sut.createLowScopedAccessToken()

        // Then
            assertTrue(result is CreateLowScopedAccessTokenResult.Failure)
            val errorMessage =
                (result as CreateLowScopedAccessTokenResult.Failure).error.errorDescription
        // The error description will be the empty body from the HTTP response
        assertEquals("", errorMessage)
    }

    @Test
    fun `createLowScopedAccessToken() includes error message in exception when available`() =
        runTest {
        // Given
        val errorMessage = "Server temporarily unavailable"
        val errorResponse = HttpResponse(
            status = 503,
            body = errorMessage
        )
        coEvery { restClient.send(any()) } returns errorResponse

        // When
            val result = sut.createLowScopedAccessToken()

        // Then
            assertTrue(result is CreateLowScopedAccessTokenResult.Failure)
            val description =
                (result as CreateLowScopedAccessTokenResult.Failure).error.errorDescription
        // The error description will be the error message from the HTTP response body
        assertEquals(errorMessage, description)
    }

    @Test
    fun `createLowScopedAccessToken() creates proper Basic Auth header from client ID`() = runTest {
        // Given
        val clientId = "test-client-123"
        val configWithCustomClientId = CoreConfig(clientId, Environment.LIVE)
        val sutWithCustomConfig =
            AuthenticationSecureTokenServiceAPI(configWithCustomClientId, restClient)

        val successResponse = HttpResponse(
            status = 200,
            body = """{"access_token": "token", "token_type": "Bearer"}"""
        )
        coEvery { restClient.send(any()) } returns successResponse

        val requestSlot = slot<APIRequest>()

        // When
        sutWithCustomConfig.createLowScopedAccessToken()

        // Then
        coVerify { restClient.send(capture(requestSlot)) }

        val authHeader = requestSlot.captured.headers!!["Authorization"]!!
        assert(authHeader.startsWith("Basic "))

        // Decode and verify the Basic auth contains the client ID
        val encodedCredentials = authHeader.substring("Basic ".length)
        val decodedClientId =
            android.util.Base64.decode(encodedCredentials, android.util.Base64.DEFAULT)
                .decodeToString()
        assertEquals(clientId, decodedClientId)
    }
}
