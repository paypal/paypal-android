package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityVariables
import com.paypal.android.corepayments.model.TokenType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.collections.mapOf

@ExperimentalCoroutinesApi
@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@RunWith(RobolectricTestRunner::class)
class PatchCCOWithAppSwitchEligibilityUnitTest {

    private lateinit var context: Context
    private lateinit var coreConfig: CoreConfig
    private lateinit var graphQLClient: GraphQLClient
    private lateinit var resourceLoader: ResourceLoader
    private lateinit var tokenServiceAPI: AuthenticationSecureTokenServiceAPI

    private lateinit var sut: PatchCCOWithAppSwitchEligibility
    private val testOrderId = "test-order-id-456"

    @Before
    fun beforeEach() {
        context = mockk(relaxed = true)
        coreConfig = CoreConfig("test-client-id", Environment.SANDBOX)
        graphQLClient = mockk(relaxed = true)
        resourceLoader = mockk(relaxed = true)
        tokenServiceAPI = mockk(relaxed = true)
        sut = PatchCCOWithAppSwitchEligibility(
            graphQLClient,
            resourceLoader,
            tokenServiceAPI
        )
        coEvery {
            tokenServiceAPI.createLowScopedAccessToken()
        } returns APIResult.Success("fake-token")
    }

    @Test
    fun `invoke sends GraphQL request with correct authorization header`() = runTest {
        // Given
        val mockQuery = "query content"
        val successResponse = createSuccessfulGraphQLResult()
        coEvery {
            resourceLoader.loadRawResource(
                any(),
                any()
            )
        } returns LoadRawResourceResult.Success(mockQuery)
        coEvery {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                any(), any()
            )
        } returns successResponse

        val requestSlot = slot<GraphQLRequest<PatchCcoWithAppSwitchEligibilityVariables>>()

        // When
        sut(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true,
            paypalNativeAppInstalled = true
        )

        // Then
        coVerify {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                capture(requestSlot), any()
            )
        }
        val capturedRequest = requestSlot.captured
        assertEquals("PatchCcoWithAppSwitchEligibility", capturedRequest.operationName)
    }

    @Test
    fun `invoke sends GraphQL request with correct variables for ORDER_ID and true merchantOptIn`() =
        runTest {
            testGraphQLRequestVariables(TokenType.ORDER_ID, true, "ORDER_ID")
        }

    @Test
    fun `invoke sends GraphQL request with correct variables for VAULT_ID and false merchantOptIn`() =
        runTest {
            testGraphQLRequestVariables(TokenType.VAULT_ID, false, "VAULT_ID")
        }

    @Test
    fun `invoke sends GraphQL request with correct variables for BILLING_TOKEN and true merchantOptIn`() =
        runTest {
            testGraphQLRequestVariables(TokenType.BILLING_TOKEN, true, "BILLING_TOKEN")
        }

    @Test
    fun `invoke sends GraphQL request with correct variables for BILLING_TOKEN and false merchantOptIn`() =
        runTest {
            testGraphQLRequestVariables(TokenType.BILLING_TOKEN, false, "BILLING_TOKEN")
        }

    private suspend fun testGraphQLRequestVariables(
        tokenType: TokenType,
        merchantOptInForAppSwitch: Boolean,
        expectedTokenTypeName: String
    ) {
        // Given
        val mockQuery = "query content"
        val successResponse = createSuccessfulGraphQLResult()
        coEvery {
            resourceLoader.loadRawResource(
                any(),
                any()
            )
        } returns LoadRawResourceResult.Success(mockQuery)
        coEvery {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                any()
            )
        } returns successResponse


        // When
        sut(
            context = context,
            orderId = testOrderId,
            tokenType = tokenType,
            merchantOptInForAppSwitch = merchantOptInForAppSwitch,
            paypalNativeAppInstalled = true
        )

        // Then
        val requestSlot = slot<GraphQLRequest<PatchCcoWithAppSwitchEligibilityVariables>>()
        coVerify {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                capture(requestSlot),
                mapOf("Authorization" to "Bearer fake-token")
            )
        }
        val capturedRequest = requestSlot.captured
        val variables = capturedRequest.variables!!

        assertEquals(expectedTokenTypeName, variables.tokenType)
        assertEquals(testOrderId, variables.contextId)
        assertEquals(testOrderId, variables.token)
        assertEquals(merchantOptInForAppSwitch, variables.merchantOptInForAppSwitch)
        assertEquals("ANDROID", variables.osType)
        assertEquals("MOBILE_SDK", variables.integrationArtifact)
        assertEquals("INCONTEXT", variables.userExperienceFlow)
    }

    @Test
    fun `invoke returns parsed response when GraphQL request succeeds`() = runTest {
        // Given
        val mockQuery = "query content"
        val successResponse = createSuccessfulGraphQLResult()
        coEvery {
            resourceLoader.loadRawResource(
                any(),
                any()
            )
        } returns LoadRawResourceResult.Success(mockQuery)
        coEvery {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                any(),
                any()
            )
        } returns successResponse

        // When
        val result = sut(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.VAULT_ID,
            merchantOptInForAppSwitch = false,
            paypalNativeAppInstalled = true
        )

        // Then
        assertTrue(result is APIResult.Success)
        assertEquals("https://paypal.com/redirect", (result as APIResult.Success).data.launchUrl)
    }

    @Test
    fun `invoke returns failure when GraphQL request fails`() = runTest {
        // Given
        val mockQuery = "query content"
        val sdkError = PayPalSDKError(1001, "Test error")
        val failureResponse = GraphQLResult.Failure(sdkError)
        coEvery {
            resourceLoader.loadRawResource(
                any(),
                any()
            )
        } returns LoadRawResourceResult.Success(mockQuery)
        coEvery {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                any(), any()
            )
        } returns failureResponse

        // When
        val result = sut(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true,
            paypalNativeAppInstalled = true
        )

        // Then
        assertTrue(result is APIResult.Failure)
        assertEquals(sdkError, (result as APIResult.Failure).error)
    }

    @Test
    fun `invoke returns failure when GraphQL response data is null`() = runTest {
        // Given
        val mockQuery = "query content"
        val successResponseWithNullData = GraphQLResult.Success(
            response = com.paypal.android.corepayments.graphql.GraphQLResponse<
                    PatchCcoWithAppSwitchEligibilityResponse
                    >(
                data = null
            ),
            correlationId = "correlation-123"
        )
        coEvery {
            resourceLoader.loadRawResource(
                any(),
                any()
            )
        } returns LoadRawResourceResult.Success(mockQuery)
        coEvery {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                any()
            )
        } returns successResponseWithNullData

        // When
        val result = sut(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true,
            paypalNativeAppInstalled = true
        )

        // Then
        assertTrue(result is APIResult.Failure)
    }

    @Test
    fun `invoke handles malformed JSON response gracefully`() = runTest {
        // Given
        val mockQuery = "query content"
        val malformedData = PatchCcoWithAppSwitchEligibilityResponse(null)
        val successResponse = GraphQLResult.Success(
            response = com.paypal.android.corepayments.graphql.GraphQLResponse(
                data = malformedData
            ),
            correlationId = "correlation-123"
        )
        coEvery {
            resourceLoader.loadRawResource(
                any(),
                any()
            )
        } returns LoadRawResourceResult.Success(mockQuery)
        coEvery {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                any()
            )
        } returns successResponse

        // When
        val result = sut(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true,
            paypalNativeAppInstalled = true
        )

        // Then
        assertTrue(result is APIResult.Failure)
    }

    @Test
    fun `invoke sets experimentation context correctly`() = runTest {
        // Given
        val mockQuery = "query content"
        val successResponse = createSuccessfulGraphQLResult()
        coEvery {
            resourceLoader.loadRawResource(any(), any())
        } returns LoadRawResourceResult.Success(mockQuery)
        coEvery {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                any()
            )
        } returns successResponse

        // When
        sut(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.BILLING_TOKEN,
            merchantOptInForAppSwitch = true,
            paypalNativeAppInstalled = true
        )

        // Then
        val requestSlot = slot<GraphQLRequest<PatchCcoWithAppSwitchEligibilityVariables>>()
        coVerify {
            graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityVariables>(
                capture(requestSlot),
                mapOf("Authorization" to "Bearer fake-token")
            )
        }
        val variables = requestSlot.captured.variables!!
        val experimentationContext = variables.experimentationContext
        assertEquals("PPCP_NATIVE_SDK", experimentationContext.integrationChannel)
    }

    // TODO: Re-enable this test when authentication is implemented
    // @Test
    // fun `invoke returns failure when authentication fails`() = runTest {
    //     // Given
    //     val sdkError = PayPalSDKError(1001, "Authentication failed")
    //     coEvery { authenticationSecureTokenServiceAPI.createLowScopedAccessToken() } returns APIResult.Failure(
    //         sdkError
    //     )
    //
    //     // When
    //     val result = sut(
    //         context = context,
    //         orderId = testOrderId,
    //         tokenType = TokenType.ORDER_ID,
    //         merchantOptInForAppSwitch = true,
    //         paypalNativeAppInstalled = true
    //     )
    //
    //     // Then
    //     assertTrue(result is APIResult.Failure)
    //     assertEquals(sdkError, (result as APIResult.Failure).error)
    //     // Verify GraphQL client was never called since authentication failed
    //     coVerify(exactly = 0) {
    //         graphQLClient.send<
    //             PatchCcoWithAppSwitchEligibilityResponse,
    //             PatchCcoWithAppSwitchEligibilityRequest
    //         >(any())
    //     }
    // }

    private fun createSuccessfulGraphQLResult(): GraphQLResult.Success<PatchCcoWithAppSwitchEligibilityResponse> {
        val responseData = PatchCcoWithAppSwitchEligibilityResponse(
            external = com.paypal.android.corepayments.model.ExternalData(
                patchCcoWithAppSwitchEligibility = com.paypal.android.corepayments.model.PatchCcoData(
                    appSwitchEligibility = com.paypal.android.corepayments.model.AppSwitchEligibilityData(
                        appSwitchEligible = true,
                        redirectURL = "https://paypal.com/redirect",
                        ineligibleReason = ""
                    )
                )
            )
        )
        return GraphQLResult.Success(
            response = com.paypal.android.corepayments.graphql.GraphQLResponse(
                data = responseData
            ),
            correlationId = "correlation-123"
        )
    }
}
