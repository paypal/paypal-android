package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.R
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.API
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.common.GraphQLClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
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
    }

    @Test
    fun checkEligibility_sendsGraphQLRequest() = runTest {
        coEvery { api.fetchCachedOrRemoteClientID() } returns "sample-client-id"

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
}