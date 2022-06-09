package com.paypal.android.core.graphql.common

import com.paypal.android.core.CoreConfig
import com.paypal.android.core.GraphQLRequestFactory
import com.paypal.android.core.Http
import com.paypal.android.core.HttpRequest
import com.paypal.android.core.HttpResponse
import com.paypal.android.core.graphql.common.GraphQLClientImpl.Companion.PAYPAL_DEBUG_ID
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibilityResponse
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection

internal class GraphQLClientImplTest {

    @MockK
    private lateinit var mockHttp: Http

    @MockK
    private lateinit var mockCoreConfig: CoreConfig

    @MockK
    private lateinit var mockGraphQLRequestFactory: GraphQLRequestFactory

    private lateinit var client: GraphQLClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        client = GraphQLClientImpl(
            coreConfig = mockCoreConfig,
            http = mockHttp,
            graphQlRequestFactory = mockGraphQLRequestFactory
        )
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
        val response = client.executeQuery(mockQuery)
        verify {
            mockQuery.parse(any())
        }
        assertNull(response.data)
    }

    @Test
    fun `verify empty response`() = runBlocking {
        val mockQuery: Query<Any> = mockk(relaxed = true)
        every { mockQuery.requestBody() } returns mockk()
        val mockHttpRequest: HttpRequest = mockk(relaxed = true)
        every { mockGraphQLRequestFactory.createHttpRequestFromQuery(any()) } returns mockHttpRequest
        coEvery { mockHttp.send(mockHttpRequest) } returns HttpResponse(
            status = HttpURLConnection.HTTP_OK,
            headers = mapOf(
                PAYPAL_DEBUG_ID to "454532"
            ),
            body = graphQlQueryResponseWithoutData
        )
        val response = client.executeQuery(mockQuery)
        assertNull(response.data)
    }

    @Test
    fun `verify non success response`() = runBlocking {
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
        val response = client.executeQuery(mockQuery)
        assertNull(response.data)
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
