package com.paypal.android.corepayments

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class GraphQLRequestFactoryTest : TestCase() {

    @MockK
    private lateinit var mockCoreConfig: CoreConfig

    private lateinit var graphQLRequestFactory: GraphQLRequestFactory

    @Before
    public override fun setUp() {
        MockKAnnotations.init(this)
        graphQLRequestFactory = GraphQLRequestFactory(mockCoreConfig)
    }

    @Test
    fun testCreateHttpRequestFromQuery() {
        every { mockCoreConfig.environment } returns Environment.SANDBOX
        val validJsonString = "{\"validKey\" : {}}"
        val requestBody = JSONObject(validJsonString)
        val result = graphQLRequestFactory.createHttpRequestFromQuery(
            requestBody = requestBody
        )
        val expectedUrl = Environment.SANDBOX.grqphQlUrl
        assertEquals(result.url.toString(), expectedUrl)
        assertEquals(result.method, HttpMethod.POST)
        assertEquals(result.body, requestBody.toString())
        assertTrue(result.headers.containsKey("Accept"))
    }
}
