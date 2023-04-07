package com.paypal.android.corepayments.graphql.common

import com.paypal.android.corepayments.Code
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.GraphQLRequestFactory
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.graphql.common.GraphQLClientImpl.Companion.PAYPAL_DEBUG_ID
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibilityResponse
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class GraphQLClientUnitTest {

    @MockK
    private lateinit var mockCoreConfig: CoreConfig

    @MockK
    private lateinit var mockGraphQLRequestFactory: GraphQLRequestFactory

    private val graphQLRequestBody = JSONObject("""{"fake":"json"}""")

    private lateinit var mockHttp: Http
    private lateinit var httpRequestSlot: CapturingSlot<HttpRequest>

    private val sandboxConfig = CoreConfig("fake-access-token", Environment.SANDBOX)
    private val stagingConfig = CoreConfig("fake-access-token", Environment.STAGING)
    private val liveConfig = CoreConfig("fake-access-token", Environment.LIVE)

    private lateinit var sut: GraphQLClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockHttp = mockk(relaxed = true)
        httpRequestSlot = slot()

        sut = GraphQLClientImpl(
            coreConfig = mockCoreConfig,
            http = mockHttp,
            graphQlRequestFactory = mockGraphQLRequestFactory
        )
    }

    @Test
    fun `send sends an http request to sandbox environment`() = runTest {
        val sut = GraphQLClientImpl(sandboxConfig, mockHttp, mockGraphQLRequestFactory)
        sut.send(graphQLRequestBody)
        coVerify { mockHttp.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://www.sandbox.paypal.com/graphql"), httpRequest.url)
        assertEquals("https://www.sandbox.paypal.com", httpRequest.headers["Origin"])
    }

    @Test
    fun `send sends an http request to staging environment`() = runTest {
        val sut = GraphQLClientImpl(stagingConfig, mockHttp, mockGraphQLRequestFactory)
        sut.send(graphQLRequestBody)
        coVerify { mockHttp.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://www.msmaster.qa.paypal.com/graphql"), httpRequest.url)
        assertEquals("https://www.msmaster.qa.paypal.com", httpRequest.headers["Origin"])
    }

    @Test
    fun `send sends an http request to live environment`() = runTest {
        val sut = GraphQLClientImpl(liveConfig, mockHttp, mockGraphQLRequestFactory)
        sut.send(graphQLRequestBody)
        coVerify { mockHttp.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(URL("https://www.paypal.com/graphql"), httpRequest.url)
        assertEquals("https://www.paypal.com", httpRequest.headers["Origin"])
    }

    @Test
    fun `send forwards graphQL request body as an http request body`() = runTest {
        val sut = GraphQLClientImpl(liveConfig, mockHttp, mockGraphQLRequestFactory)
        sut.send(graphQLRequestBody)
        coVerify { mockHttp.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals("""{"fake":"json"}""", httpRequest.body)
    }

    @Test
    fun `send sends an HTTP POST request`() = runTest {
        val sut = GraphQLClientImpl(sandboxConfig, mockHttp, mockGraphQLRequestFactory)
        sut.send(graphQLRequestBody)
        coVerify { mockHttp.send(capture(httpRequestSlot)) }

        val httpRequest = httpRequestSlot.captured
        assertEquals(HttpMethod.POST, httpRequest.method)
    }

    @Test
    fun `send sets default headers`() = runTest {
        val sut = GraphQLClientImpl(sandboxConfig, mockHttp, mockGraphQLRequestFactory)
        sut.send(graphQLRequestBody)
        coVerify { mockHttp.send(capture(httpRequestSlot)) }

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
        coEvery { mockHttp.send(any()) } returns successHttpResponse

        val sut = GraphQLClientImpl(sandboxConfig, mockHttp, mockGraphQLRequestFactory)
        val response = sut.send(graphQLRequestBody)

        assertEquals("""{"fake":"success_data"}""", response.data?.toString())
        assertEquals("fake-debug-id", response.correlationId)
    }

    @Test
    fun `send throws an error when GraphQL response is successful with an empty body`() = runTest {
        // language=JSON
        val emptyBody = ""
        val successHeaders = mapOf("Paypal-Debug-Id" to "fake-debug-id")
        val successHttpResponse = HttpResponse(200, successHeaders, emptyBody)
        coEvery { mockHttp.send(any()) } returns successHttpResponse

        val sut = GraphQLClientImpl(sandboxConfig, mockHttp, mockGraphQLRequestFactory)
        try {
            sut.send(graphQLRequestBody)
        } catch (e: PayPalSDKError) {
            assertEquals(Code.NO_RESPONSE_DATA.ordinal, e.code)
            val expectedErrorMessage =
                "An error occurred due to missing HTTP response data. Contact developer.paypal.com/support."
            assertEquals(expectedErrorMessage, e.errorDescription)
            assertEquals("fake-debug-id", e.correlationID)
        }
    }

    @Test
    fun `verify non empty response`() = runBlocking {
        val mockQuery: Query<Any> = mockk(relaxed = true)
        every { mockQuery.requestBody() } returns mockk()
        val mockHttpRequest: HttpRequest = mockk(relaxed = true)
        val fundingEligibilityResponse: FundingEligibilityResponse = mockk(relaxed = true)
        every { mockGraphQLRequestFactory.createHttpRequestFromQuery(any()) } returns mockHttpRequest
        every { mockQuery.parse(any()) } returns fundingEligibilityResponse
        coEvery { mockHttp.send(mockHttpRequest) } returns HttpResponse(
            status = HttpURLConnection.HTTP_OK,
            headers = mapOf(
                PAYPAL_DEBUG_ID to "454532"
            ),
            body = graphQlQueryResponseWithData
        )
        val response = sut.executeQuery(mockQuery)
        verify {
            mockQuery.parse(any())
        }
        assertNotNull(response.data)
    }

    @Test
    fun `verify non success response`(): Unit = runBlocking {
        val mockQuery: Query<Any> = mockk(relaxed = true)
        every { mockQuery.requestBody() } returns mockk()
        val mockHttpRequest: HttpRequest = mockk(relaxed = true)
        every { mockGraphQLRequestFactory.createHttpRequestFromQuery(any()) } returns mockHttpRequest
        coEvery { mockHttp.send(mockHttpRequest) } returns HttpResponse(
            status = HttpURLConnection.HTTP_BAD_REQUEST,
            headers = mapOf(
                PAYPAL_DEBUG_ID to "454532"
            ),
            body = graphQlQueryResponseWithoutData
        )
        val result = sut.executeQuery(mockQuery)
        assertNull(result.data)
    }

    companion object {
        const val graphQlQueryResponseWithData = """
          { "data": {
            }
          }
        """

        const val graphQlQueryResponseWithoutData = """
          { 
          }
        """
    }
}
