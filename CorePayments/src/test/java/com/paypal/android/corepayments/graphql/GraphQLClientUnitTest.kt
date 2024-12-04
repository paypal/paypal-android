package com.paypal.android.corepayments.graphql

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.PayPalSDKErrorCode
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class GraphQLClientUnitTest {

    private val sandboxConfig = CoreConfig("fake-client-id", Environment.SANDBOX)
    private val liveConfig = CoreConfig("fake-client-id", Environment.LIVE)

    private val graphQLRequestBody = JSONObject("""{"fake":"json"}""")

    private lateinit var http: Http
    private lateinit var httpRequestSlot: CapturingSlot<HttpRequest>

    private lateinit var sut: GraphQLClient

    @Before
    fun setUp() {
        http = mockk(relaxed = true)
        httpRequestSlot = slot()
    }

    @Test
    fun `send sends an http request to sandbox environment`() = runTest {
        sut = GraphQLClient(sandboxConfig, http)
        sut.send(graphQLRequestBody)
        coVerify { http.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://www.sandbox.paypal.com/graphql"), httpRequest.url)
        assertEquals("https://www.sandbox.paypal.com", httpRequest.headers["Origin"])
    }

    @Test
    fun `send sends an http request to sandbox environment with query name appended`() = runTest {
        sut = GraphQLClient(sandboxConfig, http)
        sut.send(graphQLRequestBody, "QueryName")
        coVerify { http.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://www.sandbox.paypal.com/graphql?QueryName"), httpRequest.url)
        assertEquals("https://www.sandbox.paypal.com", httpRequest.headers["Origin"])
    }

    @Test
    fun `send sends an http request to live environment`() = runTest {
        sut = GraphQLClient(liveConfig, http)
        sut.send(graphQLRequestBody)
        coVerify { http.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://www.paypal.com/graphql"), httpRequest.url)
        assertEquals("https://www.paypal.com", httpRequest.headers["Origin"])
    }

    @Test
    fun `send sends an http request to live environment with query name appended`() = runTest {
        sut = GraphQLClient(liveConfig, http)
        sut.send(graphQLRequestBody, "QueryName")
        coVerify { http.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://www.paypal.com/graphql?QueryName"), httpRequest.url)
        assertEquals("https://www.paypal.com", httpRequest.headers["Origin"])
    }

    @Test
    fun `send forwards graphQL request body as an http request body`() = runTest {
        sut = GraphQLClient(liveConfig, http)
        sut.send(graphQLRequestBody)
        coVerify { http.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals("""{"fake":"json"}""", httpRequest.body)
    }

    @Test
    fun `send sends an HTTP POST request`() = runTest {
        sut = GraphQLClient(sandboxConfig, http)
        sut.send(graphQLRequestBody)
        coVerify { http.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(HttpMethod.POST, httpRequest.method)
    }

    @Test
    fun `send sets default headers`() = runTest {
        sut = GraphQLClient(sandboxConfig, http)
        sut.send(graphQLRequestBody)
        coVerify { http.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals("application/json", httpRequest.headers["Content-Type"])
        assertEquals("application/json", httpRequest.headers["Accept"])
        assertEquals("nativecheckout", httpRequest.headers["x-app-name"])
    }

    @Test
    fun `send parses GraphQL success response`() = runTest {
        // language=JSON
        val successBody = """{ "data": { "fake": "success_data" } }"""
        val successHeaders = mapOf("Paypal-Debug-Id" to "fake-debug-id")
        val successHttpResponse = HttpResponse(200, successHeaders, successBody)
        coEvery { http.send(any()) } returns successHttpResponse

        sut = GraphQLClient(sandboxConfig, http)
        val result = sut.send(graphQLRequestBody) as GraphQLResult.Success

        assertEquals("""{"fake":"success_data"}""", result.data?.toString())
        assertEquals("fake-debug-id", result.correlationId)
    }

    @Test
    fun `send returns an error when GraphQL response is successful with an empty body`() = runTest {
        // language=JSON
        val emptyBody = ""
        val successHeaders = mapOf("Paypal-Debug-Id" to "fake-debug-id")
        val successHttpResponse = HttpResponse(200, successHeaders, emptyBody)
        coEvery { http.send(any()) } returns successHttpResponse

        sut = GraphQLClient(sandboxConfig, http)
        val result = sut.send(graphQLRequestBody) as GraphQLResult.Failure
        assertEquals(PayPalSDKErrorCode.NO_RESPONSE_DATA.ordinal, result.error.code)

        val expectedErrorMessage =
            "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support."
        assertEquals(expectedErrorMessage, result.error.errorDescription)
        assertEquals("fake-debug-id", result.error.correlationId)
    }

    @Test
    fun `send returns an error when GraphQL response is successful with an invalid JSON body`() =
        runTest {
            val invalidJSON = """{ invalid: """
            val successHeaders = mapOf("Paypal-Debug-Id" to "fake-debug-id")
            val successHttpResponse = HttpResponse(200, successHeaders, invalidJSON)
            coEvery { http.send(any()) } returns successHttpResponse

            sut = GraphQLClient(sandboxConfig, http)
            val result = sut.send(graphQLRequestBody) as GraphQLResult.Failure
            assertEquals(PayPalSDKErrorCode.GRAPHQL_JSON_INVALID_ERROR.ordinal, result.error.code)

            val expectedErrorMessage =
                "An error occurred while parsing the GraphQL response JSON. Contact developer.paypal.com/support."
            assertEquals(expectedErrorMessage, result.error.errorDescription)
            assertEquals("fake-debug-id", result.error.correlationId)
        }
}
