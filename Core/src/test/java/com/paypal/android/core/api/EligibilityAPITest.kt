package com.paypal.android.core.api

import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.assertThrows
import com.paypal.android.core.graphql.common.GraphQLClient
import com.paypal.android.core.graphql.common.GraphQlQueryResponse
import com.paypal.android.core.graphql.common.Query
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibility
import com.paypal.android.core.graphql.fundingEligibility.models.FundingEligibilityResponse
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class EligibilityAPITest : TestCase() {

    lateinit var api: EligibilityAPI

    @MockK
    private lateinit var mockCoreConfig: CoreConfig

    @MockK
    private lateinit var mockkGraphQlClient: GraphQLClient

    @Before
    public override fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        api = EligibilityAPI(mockCoreConfig, "", mockkGraphQlClient)
    }

    @Test
    fun testCheckEligibilitySuccessCase() = runBlocking {
        val mockFundingEligibilityResponse: FundingEligibilityResponse = mockk(relaxed = true)
        every { mockFundingEligibilityResponse.fundingEligibility.venmo.eligible } returns true
        coEvery { mockkGraphQlClient.executeQuery(any<Query<FundingEligibilityResponse>>()) } returns
                GraphQlQueryResponse(data = mockFundingEligibilityResponse)
        val result = api.checkEligibility()
        assertEquals(result.isVenmoEligible, true)
    }

    @Test
    fun testCheckEligibilityErrorCase(): Unit = runBlocking {
        coEvery { mockkGraphQlClient.executeQuery(any<Query<FundingEligibility>>()) } returns
                GraphQlQueryResponse(correlationId = "correlationId")
        assertThrows<PayPalSDKError> { api.checkEligibility() }
    }
}
