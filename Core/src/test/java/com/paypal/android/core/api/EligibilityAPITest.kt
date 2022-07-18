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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class EligibilityAPITest {

    lateinit var api: EligibilityAPI

    @MockK
    private lateinit var mockCoreConfig: CoreConfig

    @MockK
    private lateinit var mockkGraphQlClient: GraphQLClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        api = EligibilityAPI(mockCoreConfig, mockkGraphQlClient)
    }

    @Test
    fun `a successful eligibility check`() = runBlocking {
        val mockFundingEligibilityResponse: FundingEligibilityResponse = mockk(relaxed = true)
        every { mockFundingEligibilityResponse.fundingEligibility.venmo.eligible } returns true
        every { mockCoreConfig.clientId } returns "mock_client_id"
        coEvery { mockkGraphQlClient.executeQuery(any<Query<FundingEligibilityResponse>>()) } returns
                GraphQlQueryResponse(data = mockFundingEligibilityResponse)
        val result = api.checkEligibility()
        assertEquals(result.isVenmoEligible, true)
    }

    @Test
    fun `an unsuccessful eligibility check`() = runBlocking {
        val correlationId = "correlationId"
        every { mockCoreConfig.clientId } returns "mock_client_id"
        coEvery { mockkGraphQlClient.executeQuery(any<Query<FundingEligibility>>()) } returns
                GraphQlQueryResponse(correlationId = correlationId)
        val exception = assertThrows<PayPalSDKError> { api.checkEligibility() }
        assertEquals(exception.correlationID, correlationId)
    }

    @Test
    fun `when client id is null, it should throw exception`() = runBlocking {
        val exception = assertThrows<PayPalSDKError> { api.checkEligibility() }
        assertEquals("Client Id should not be null or empty", exception.errorDescription)
    }
}
