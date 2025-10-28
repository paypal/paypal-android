package com.paypal.android.corepayments

import android.content.Context
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResponse
import com.paypal.android.corepayments.graphql.GraphQLResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@OptIn(InternalSerializationApi::class)
@RunWith(RobolectricTestRunner::class)
class UpdateClientConfigAPIUnitTest {

    // Test constants
    private val testOrderId = "test-order-id"
    private val testFundingSource = "paypal"
    private val testCorrelationId = "test-correlation-id"
    private val testGraphQLQuery = "mutation UpdateClientConfig { updateClientConfig }"

    // Mocks
    private lateinit var mockContext: Context
    private lateinit var mockGraphQLClient: GraphQLClient
    private lateinit var mockResourceLoader: ResourceLoader
    private lateinit var updateClientConfigAPI: UpdateClientConfigAPI

    @Before
    fun beforeEach() {
        mockContext = mockk(relaxed = true)
        mockGraphQLClient = mockk(relaxed = true)
        mockResourceLoader = mockk(relaxed = true)

        updateClientConfigAPI = UpdateClientConfigAPI(
            applicationContext = mockContext,
            graphQLClient = mockGraphQLClient,
            resourceLoader = mockResourceLoader
        )
    }

    @Test
    fun `updateClientConfig() returns Success when resource loading and GraphQL call succeed with valid data`() =
        runTest {
            // Arrange
            val mockResponse = UpdateClientConfigResponse("success")
            val mockGraphQLResponse = GraphQLResponse(data = mockResponse)
            val mockGraphQLResult = GraphQLResult.Success(mockGraphQLResponse, testCorrelationId)

            coEvery {
                mockResourceLoader.loadRawResource(any(), any())
            } returns LoadRawResourceResult.Success(testGraphQLQuery)

            coEvery {
                mockGraphQLClient.send<UpdateClientConfigResponse, UpdateClientConfigVariables>(any())
            } returns mockGraphQLResult

            // Act
            val result = updateClientConfigAPI.updateClientConfig(testOrderId, testFundingSource)

            // Assert
            assertTrue("Result should be Success", result is UpdateClientConfigResult.Success)

            // Verify resource loading was called with correct parameters
            coVerify {
                mockResourceLoader.loadRawResource(
                    mockContext,
                    R.raw.graphql_query_update_client_config
                )
            }

            // Verify GraphQL request was created and sent correctly
            val requestSlot = slot<GraphQLRequest<UpdateClientConfigVariables>>()
            coVerify {
                mockGraphQLClient.send<UpdateClientConfigResponse, UpdateClientConfigVariables>(
                    capture(requestSlot)
                )
            }

            val capturedRequest = requestSlot.captured
            assertEquals("Query should match", testGraphQLQuery, capturedRequest.query)
            assertEquals(
                "Operation name should be correct",
                "UpdateClientConfig",
                capturedRequest.operationName
            )

            val variables = capturedRequest.variables
            assertNotNull("Variables should not be null", variables)
            assertEquals("Order ID should match", testOrderId, variables!!.orderID)
            assertEquals("Funding source should match", testFundingSource, variables.fundingSource)
            assertEquals(
                "Integration artifact should be default",
                UpdateClientConfigAPI.Defaults.INTEGRATION_ARTIFACT,
                variables.integrationArtifact
            )
            assertEquals(
                "User experience flow should be default",
                UpdateClientConfigAPI.Defaults.USER_EXPERIENCE_FLOW,
                variables.userExperienceFlow
            )
            assertEquals(
                "Product flow should be default",
                UpdateClientConfigAPI.Defaults.PRODUCT_FLOW,
                variables.productFlow
            )
            assertEquals("Button session ID should be null", null, variables.buttonSessionId)
        }

    @Test
    fun `updateClientConfig() returns Success when GraphQL response has valid data`() = runTest {
        // Arrange
        val mockResponse = UpdateClientConfigResponse("updated")
        val mockGraphQLResponse = GraphQLResponse(data = mockResponse)
        val mockGraphQLResult = GraphQLResult.Success(mockGraphQLResponse, testCorrelationId)

        coEvery {
            mockResourceLoader.loadRawResource(any(), any())
        } returns LoadRawResourceResult.Success(testGraphQLQuery)

        coEvery {
            mockGraphQLClient.send<UpdateClientConfigResponse, UpdateClientConfigVariables>(any())
        } returns mockGraphQLResult

        // Act
        val result = updateClientConfigAPI.updateClientConfig(testOrderId, testFundingSource)

        // Assert
        assertTrue("Result should be Success", result is UpdateClientConfigResult.Success)
    }

    @Test
    fun `updateClientConfig() returns Failure when GraphQL response has null data`() = runTest {
        // Arrange
        val mockGraphQLResponse = GraphQLResponse<UpdateClientConfigResponse>(data = null)
        val mockGraphQLResult = GraphQLResult.Success(mockGraphQLResponse, testCorrelationId)

        coEvery {
            mockResourceLoader.loadRawResource(any(), any())
        } returns LoadRawResourceResult.Success(testGraphQLQuery)

        coEvery {
            mockGraphQLClient.send<UpdateClientConfigResponse, UpdateClientConfigVariables>(any())
        } returns mockGraphQLResult

        // Act
        val result = updateClientConfigAPI.updateClientConfig(testOrderId, testFundingSource)

        // Assert
        assertTrue("Result should be Failure", result is UpdateClientConfigResult.Failure)
        val failure = result as UpdateClientConfigResult.Failure
        assertEquals(
            "Error code should be NO_RESPONSE_DATA",
            PayPalSDKErrorCode.NO_RESPONSE_DATA.ordinal,
            failure.error.code
        )
        assertEquals("Correlation ID should match", testCorrelationId, failure.error.correlationId)
        assertTrue(
            "Error description should mention missing data",
            failure.error.errorDescription.contains("missing HTTP response data")
        )
    }

    @Test
    fun `updateClientConfig() returns Failure when GraphQL call fails`() = runTest {
        // Arrange
        val graphQLError = PayPalSDKError(123, "GraphQL error")
        val mockGraphQLResult = GraphQLResult.Failure(graphQLError)

        coEvery {
            mockResourceLoader.loadRawResource(any(), any())
        } returns LoadRawResourceResult.Success(testGraphQLQuery)

        coEvery {
            mockGraphQLClient.send<UpdateClientConfigResponse, UpdateClientConfigVariables>(any())
        } returns mockGraphQLResult

        // Act
        val result = updateClientConfigAPI.updateClientConfig(testOrderId, testFundingSource)

        // Assert
        assertTrue("Result should be Failure", result is UpdateClientConfigResult.Failure)
        val failure = result as UpdateClientConfigResult.Failure
        assertEquals("Error should be the GraphQL error", graphQLError, failure.error)
    }

    @Test
    fun `updateClientConfig() returns Failure when resource loading fails`() = runTest {
        // Arrange
        val resourceError = PayPalSDKError(456, "Resource loading failed")
        coEvery {
            mockResourceLoader.loadRawResource(any(), any())
        } returns LoadRawResourceResult.Failure(resourceError)

        // Act
        val result = updateClientConfigAPI.updateClientConfig(testOrderId, testFundingSource)

        // Assert
        assertTrue("Result should be Failure", result is UpdateClientConfigResult.Failure)
        val failure = result as UpdateClientConfigResult.Failure
        assertEquals("Error code should be 0", 0, failure.error.code)
        assertTrue(
            "Error description should mention GraphQL query resource",
            failure.error.errorDescription.contains("Failed to load GraphQL query resource")
        )

        // Verify GraphQL client was not called when resource loading fails
        coVerify(exactly = 0) {
            mockGraphQLClient.send<UpdateClientConfigResponse, UpdateClientConfigVariables>(any())
        }
    }

}