package com.paypal.android.corepayments.graphql

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.PayPalSDKErrorCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@ExperimentalCoroutinesApi
@OptIn(InternalSerializationApi::class)
@RunWith(RobolectricTestRunner::class)
internal class GraphQLClientUnitTest {

    // Test data and fixtures
    @Serializable
    private data class TestRequest(val id: String)

    @Serializable
    private data class TestResponse(val result: String)

    // Constants for tests
    private val testQuery = "query { test }"
    private val testVariables = TestRequest("test-id")
    private val testOperationName = "TestOperation"
    private val testCorrelationId = "test-correlation-id"

    // Mocks
    private lateinit var mockHttp: Http
    private lateinit var mockCoreConfig: CoreConfig
    private lateinit var graphQLClient: GraphQLClient
    @Before
    fun setup() {
        mockHttp = mockk(relaxed = true)
        mockCoreConfig = mockk(relaxed = true)

        // Setup mock environment for the CoreConfig
        val mockEnvironment = mockk<Environment>(relaxed = true)
        every { mockEnvironment.graphQLEndpoint } returns "https://test-api.paypal.com"
        every { mockCoreConfig.environment } returns mockEnvironment

        // Create the GraphQLClient with mocked dependencies
        graphQLClient = GraphQLClient(mockCoreConfig, mockHttp)
    }

    @Test
    fun `test successful GraphQL request and response`() = runTest {
        // Arrange
        val expectedUrl = URL("https://test-api.paypal.com/graphql?$testOperationName")
        val graphQLRequest = GraphQLRequest(
            query = testQuery,
            variables = testVariables,
            operationName = testOperationName
        )

        // Mock HTTP response
        val successfulResponseJson = """
            {
                "data": {
                    "result": "success"
                }
            }
        """.trimIndent()

        val headers = mapOf(GraphQLClient.PAYPAL_DEBUG_ID to testCorrelationId)
        val mockResponse = HttpResponse(
            status = 200,
            body = successfulResponseJson,
            headers = headers
        )

        // Capture the HttpRequest to verify it later
        val httpRequestSlot = slot<HttpRequest>()
        coEvery { mockHttp.send(capture(httpRequestSlot)) } returns mockResponse

        // Act
        val result = graphQLClient.send<TestResponse, TestRequest>(
            graphQLRequest
        )

        // Assert
        assertTrue(result is GraphQLResult.Success)
        val successResult = result as GraphQLResult.Success<TestResponse>

        // Verify response data
        assertEquals(testCorrelationId, successResult.correlationId)
        assertNotNull(successResult.response.data)
        assertEquals("success", successResult.response.data?.result)

        // Verify the HTTP request was created correctly
        val capturedRequest = httpRequestSlot.captured
        assertEquals(expectedUrl, capturedRequest.url)
        assertEquals(HttpMethod.POST, capturedRequest.method)
        val expectedRequestBody = """{"query":"$testQuery","variables":{"id":"test-id"}}"""
        assertEquals(expectedRequestBody, capturedRequest.body)

        // Verify HTTP send was called
        coVerify(exactly = 1) { mockHttp.send(any()) }
    }

    @OptIn(InternalSerializationApi::class)
    @Test
    fun `test HTTP error response handling`() = runTest {
        // Arrange
        val graphQLRequest = GraphQLRequest(
            query = testQuery,
            variables = testVariables
        )

        // Create an HTTP error response (non-200 status code)
        val headers = mapOf(GraphQLClient.PAYPAL_DEBUG_ID to testCorrelationId)
        val mockResponse = HttpResponse(
            status = 400, // Bad Request
            body = null,
            headers = headers
        )

        coEvery { mockHttp.send(any()) } returns mockResponse

        // Act
        val result = graphQLClient.send<TestResponse, TestRequest>(
            graphQLRequest
        )

        // Assert
        assertTrue(result is GraphQLResult.Success)
        val successResult = result as GraphQLResult.Success<TestResponse>

        // Verify we get a response object with null data
        assertEquals(testCorrelationId, successResult.correlationId)
        assertNull(successResult.response.data)
        assertNull(successResult.response.errors)
        assertNull(successResult.response.extensions)

        // Verify HTTP send was called
        coVerify(exactly = 1) { mockHttp.send(any()) }
    }

    @Test
    fun `test empty response body handling`() = runTest {
        // Arrange
        val graphQLRequest = GraphQLRequest(
            query = testQuery,
            variables = testVariables
        )

        // Create a response with empty body but 200 status
        val headers = mapOf(GraphQLClient.PAYPAL_DEBUG_ID to testCorrelationId)
        val mockResponse = HttpResponse(
            status = 200,
            body = "", // Empty body
            headers = headers
        )

        coEvery { mockHttp.send(any()) } returns mockResponse

        // Act
        val result = graphQLClient.send<TestResponse, TestRequest>(
            graphQLRequest
        )

        // Assert
        assertTrue(result is GraphQLResult.Failure)
        val failureResult = result as GraphQLResult.Failure

        // Verify we get an error about no response data
        assertEquals(PayPalSDKErrorCode.NO_RESPONSE_DATA.ordinal, failureResult.error.code)
        assertEquals(testCorrelationId, failureResult.error.correlationId)

        // Verify HTTP send was called
        coVerify(exactly = 1) { mockHttp.send(any()) }
    }

    @Test
    fun `test JSON parse error handling`() = runTest {
        // Arrange
        val graphQLRequest = GraphQLRequest(
            query = testQuery,
            variables = testVariables
        )

        // Create a response with invalid JSON
        val headers = mapOf(GraphQLClient.PAYPAL_DEBUG_ID to testCorrelationId)
        val mockResponse = HttpResponse(
            status = 200,
            body = "{invalid json that will cause parsing error", // Invalid JSON
            headers = headers
        )

        coEvery { mockHttp.send(any()) } returns mockResponse

        // Act
        val result = graphQLClient.send<TestResponse, TestRequest>(
            graphQLRequest
        )

        // Assert
        assertTrue(result is GraphQLResult.Failure)
        val failureResult = result as GraphQLResult.Failure

        // Verify we get an error about JSON parsing
        assertEquals(
            PayPalSDKErrorCode.GRAPHQL_JSON_INVALID_ERROR.ordinal,
            failureResult.error.code
        )
        assertEquals(testCorrelationId, failureResult.error.correlationId)

        // Verify HTTP send was called
        coVerify(exactly = 1) { mockHttp.send(any()) }
    }

    @Test
    fun `test invalid URL handling`() = runTest {
        // Arrange - setup a situation where creating the URL will fail
        // We'll use a mock environment with an invalid URL format
        val invalidMockEnvironment = mockk<Environment>(relaxed = true)
        every { invalidMockEnvironment.graphQLEndpoint } returns "invalid://endpoint"

        val invalidMockCoreConfig = mockk<CoreConfig>(relaxed = true)
        every { invalidMockCoreConfig.environment } returns invalidMockEnvironment

        val invalidUrlClient = GraphQLClient(invalidMockCoreConfig, mockHttp)

        val graphQLRequest = GraphQLRequest(
            query = testQuery,
            variables = testVariables,
            operationName = "$" // Invalid character in URL
        )

        // Act
        val result = invalidUrlClient.send<TestResponse, TestRequest>(
            graphQLRequest
        )

        // Assert
        assertTrue(result is GraphQLResult.Failure)
        val failureResult = result as GraphQLResult.Failure

        // Verify we get an error about invalid URL
        assertEquals(PayPalSDKErrorCode.INVALID_URL_REQUEST.ordinal, failureResult.error.code)

        // Verify HTTP send was not called (since URL creation failed)
        coVerify(exactly = 0) { mockHttp.send(any()) }
    }

    @Test
    fun `test request serialization failure`() = runTest {

        data class NonSerializable(val s: String)

        val graphQLRequest = GraphQLRequest(
            query = testQuery,
            variables = NonSerializable("non serializable")
        )

        // Act
        val result: GraphQLResult<TestResponse> = graphQLClient.send(graphQLRequest)

        // Assert
        assertTrue(result is GraphQLResult.Failure)
        val failureResult = result as GraphQLResult.Failure
        assertEquals(PayPalSDKErrorCode.UNKNOWN.ordinal, failureResult.error.code)

        // Verify HTTP send was NOT called since the request creation failed
        coVerify(exactly = 0) { mockHttp.send(any()) }
    }
}
