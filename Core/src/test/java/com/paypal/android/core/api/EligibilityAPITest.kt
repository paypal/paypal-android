package com.paypal.android.core.api

import com.paypal.android.core.API
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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class EligibilityAPITest {

    @MockK
    private lateinit var api: API

    @MockK
    private lateinit var graphQLClient: GraphQLClient

    lateinit var sut: EligibilityAPI

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        sut = EligibilityAPI(api, graphQLClient)
    }

    @Test
    fun `a successful eligibility check`() = runBlocking {
        val mockFundingEligibilityResponse: FundingEligibilityResponse = mockk(relaxed = true)
        every { mockFundingEligibilityResponse.fundingEligibility.venmo.eligible } returns true
        coEvery { api.getClientId() } returns "mock_client_id"
        coEvery { graphQLClient.executeQuery(any<Query<FundingEligibilityResponse>>()) } returns
                GraphQlQueryResponse(data = mockFundingEligibilityResponse)
        val result = sut.checkEligibility()
        assertEquals(result.isVenmoEligible, true)
    }

    @Test
    fun `an unsuccessful eligibility check`() = runBlocking {
        val correlationId = "correlationId"
        coEvery { api.getClientId() } returns "mock_client_id"
        coEvery { graphQLClient.executeQuery(any<Query<FundingEligibility>>()) } returns
                GraphQlQueryResponse(correlationId = correlationId)
        val exception = assertThrows<PayPalSDKError> { sut.checkEligibility() }
        assertEquals(exception.correlationID, correlationId)
    }

    @Test
    fun `propagates errors from core api`() = runBlocking {
        val error = Exception("client id error")
        coEvery { api.getClientId() } throws error

        var capturedError: Exception? = null
        try {
            sut.checkEligibility()
        } catch (e: Exception) {
            capturedError = e
        }
        assertSame(error, capturedError)
    }
}
