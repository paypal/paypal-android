package com.paypal.android.corepayments.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.API
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.common.GraphQLClient
import com.paypal.android.corepayments.graphql.common.GraphQLQueryResponse
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedPaymentMethodsTypeEligibility
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class EligibilityAPIUnitTest {

    private lateinit var api: API
    private lateinit var graphQLClient: GraphQLClient

    private lateinit var sut: EligibilityAPI

    // Ref: https://stackoverflow.com/a/58617596
    private val resourceLoader =
        ResourceLoader(ApplicationProvider.getApplicationContext<Application>())

    @Before
    fun beforeEach() {
        api = mockk(relaxed = true)
        graphQLClient = mockk(relaxed = true)

        coEvery { api.fetchCachedOrRemoteClientID() } returns "sample-client-id"
    }

    @Test
    fun checkEligibility_sendsGraphQLRequest() = runTest {
        sut = EligibilityAPI(api, graphQLClient, resourceLoader)
        sut.checkEligibility()

        val requestBodySlot = slot<JSONObject>()
        coVerify { graphQLClient.send(capture(requestBodySlot)) }
        val actualRequestBody = requestBodySlot.captured

        val expectedQuery = resourceLoader.loadRawResource(R.raw.graphql_query_funding_eligibility)
        // language=JSON
        val expectedRequestBody = """
        {
            "query": "$expectedQuery",
            "variables": {
              "clientId": "sample-client-id",
              "intent": "CAPTURE",
              "currency": "USD",
              "enableFunding": ["VENMO"]
            }
        }
        """

        JSONAssert.assertEquals(JSONObject(expectedRequestBody), actualRequestBody, true)
    }

    @Test
    fun checkEligibility_parsesGraphQLResponse() = runTest {
        // language=JSON
        val data = """
            {
              "fundingEligibility": {
                "venmo": { "eligible": true },
                "card": { "eligible": true },
                "paypal": { "eligible": true },
                "paylater": { "eligible": true },
                "credit": { "eligible": true }
              }
            }
        """
        val graphQLResponse = GraphQLQueryResponse(JSONObject(data))
        coEvery { graphQLClient.send(any()) } returns graphQLResponse

        sut = EligibilityAPI(api, graphQLClient, resourceLoader)
        val result = sut.checkEligibility()

        assertTrue(result.isCreditCardEligible)
        assertTrue(result.isVenmoEligible)
        assertTrue(result.isPayLaterEligible)
        assertTrue(result.isPaypalCreditEligible)
        assertTrue(result.isPaypalEligible)
    }
}