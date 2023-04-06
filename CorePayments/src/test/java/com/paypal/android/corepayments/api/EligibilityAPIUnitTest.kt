package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.R
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.API
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.common.GraphQLClient
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        sut = EligibilityAPI(api, graphQLClient)

        sut.checkEligibility()

        val requestBodySlot = slot<JSONObject>()
        coVerify { graphQLClient.send(capture(requestBodySlot)) }
        val requestBody = requestBodySlot.captured

        val expectedQuery = resourceLoader.loadRawResource(R.raw.graphql_query_funding_eligibility)
        val expectedVariables = JSONObject()
        val expectedJSON = JSONObject()
            .put("query", expectedQuery)
            .put("variables", expectedVariables)
    }
}