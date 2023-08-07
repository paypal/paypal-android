package com.paypal.android.corepayments.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.common.GraphQLClient
import com.paypal.android.corepayments.graphql.common.GraphQLResponse
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

    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)

    private lateinit var graphQLClient: GraphQLClient

    private lateinit var sut: EligibilityAPI

    // Ref: https://stackoverflow.com/a/58617596
    private val resourceLoader = ResourceLoader()

    private lateinit var context: Context

    @Before
    fun beforeEach() {
        context = ApplicationProvider.getApplicationContext()
        graphQLClient = mockk(relaxed = true)
    }

    @Test
    fun checkEligibility_sendsGraphQLRequest() = runTest {
        sut = EligibilityAPI(coreConfig, graphQLClient, resourceLoader)
        sut.checkEligibility(context)

        val requestBodySlot = slot<JSONObject>()
        coVerify { graphQLClient.send(capture(requestBodySlot)) }
        val actualRequestBody = requestBodySlot.captured

        val expectedQuery =
            resourceLoader.loadRawResource(context, R.raw.graphql_query_funding_eligibility)
        // language=JSON
        val expectedRequestBody = """
        {
            "query": "$expectedQuery",
            "variables": {
              "clientId": "fake-client-id",
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
        val graphQLResponse = GraphQLResponse(JSONObject(data))
        coEvery { graphQLClient.send(any()) } returns graphQLResponse

        sut = EligibilityAPI(coreConfig, graphQLClient, resourceLoader)
        val result = sut.checkEligibility(context)

        assertTrue(result.isCreditCardEligible)
        assertTrue(result.isVenmoEligible)
        assertTrue(result.isPayLaterEligible)
        assertTrue(result.isPaypalCreditEligible)
        assertTrue(result.isPaypalEligible)
    }
}
