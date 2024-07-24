package com.paypal.android.corepayments.apis.eligibility

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.OrderIntent
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.apis.eligibility.EligibilityAPI
import com.paypal.android.corepayments.features.eligibility.EligibilityRequest
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class EligibilityAPIUnitTest {

    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)

    private val resourceLoader = ResourceLoader()
    private val context = ApplicationProvider.getApplicationContext<Application>()

    private lateinit var graphQLClient: GraphQLClient
    private lateinit var sut: EligibilityAPI

    @Before
    fun beforeEach() {
        graphQLClient = mockk(relaxed = true)
    }

    @Test
    fun checkEligibility_sendsGraphQLRequest() = runTest {
        sut = EligibilityAPI(coreConfig, graphQLClient, resourceLoader)
        sut.checkEligibility(context, EligibilityRequest(OrderIntent.CAPTURE, "USD"))

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
        val result = sut.checkEligibility(context, EligibilityRequest(OrderIntent.AUTHORIZE, "USD"))

        Assert.assertTrue(result.isCreditCardEligible)
        Assert.assertTrue(result.isVenmoEligible)
        Assert.assertTrue(result.isPayLaterEligible)
        Assert.assertTrue(result.isPayPalCreditEligible)
        Assert.assertTrue(result.isPayPalEligible)
    }
}
