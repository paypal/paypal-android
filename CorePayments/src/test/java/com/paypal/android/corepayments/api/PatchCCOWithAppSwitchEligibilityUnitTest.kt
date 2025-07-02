package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.TokenType
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PatchCCOWithAppSwitchEligibilityUnitTest {

    private lateinit var context: Context
    private lateinit var coreConfig: CoreConfig
    private lateinit var graphQLClient: GraphQLClient
    private lateinit var authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI
    private lateinit var sut: PatchCCOWithAppSwitchEligibility

    private val testToken = "test-token-123"
    private val testOrderId = "test-order-id-456"

    @Before
    fun beforeEach() {
        context = mockk(relaxed = true)
        coreConfig = CoreConfig("test-client-id", Environment.SANDBOX)
        graphQLClient = mockk(relaxed = true)
        authenticationSecureTokenServiceAPI = mockk(relaxed = true)
        sut = PatchCCOWithAppSwitchEligibility(
            coreConfig,
            authenticationSecureTokenServiceAPI,
            graphQLClient
        )
    }

    @Test
    fun `invoke sends GraphQL request with correct authorization header`() = runTest {
        // Given
        val successResponse = createSuccessfulGraphQLResult()
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns successResponse

        val headersSlot = slot<Map<String, String>>()

        // When
        sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true
        )

        // Then
        coVerify { graphQLClient.send(any(), any(), capture(headersSlot)) }
        val capturedHeaders = headersSlot.captured
        assertEquals("Bearer $testToken", capturedHeaders["Authorization"])
    }

    @Test
    fun `invoke sends GraphQL request with correct variables`() = runTest {
        // Given
        val successResponse = createSuccessfulGraphQLResult()
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns successResponse

        val requestSlot = slot<JSONObject>()

        // When
        sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true
        )

        // Then
        coVerify { graphQLClient.send(capture(requestSlot), any(), any()) }
        val capturedRequest = requestSlot.captured
        val variables = capturedRequest.getJSONObject("variables")

        assertEquals("ORDER_ID", variables.getString("tokenType"))
        assertEquals(testOrderId, variables.getString("contextId"))
        assertEquals(testOrderId, variables.getString("token"))
        assertEquals(true, variables.getBoolean("merchantOptInForAppSwitch"))
        assertEquals("ANDROID", variables.getString("osType"))
        assertEquals("NATIVE_SDK", variables.getString("integrationArtifact"))
    }

    @Test
    fun `invoke returns parsed response when GraphQL request succeeds`() = runTest {
        // Given
        val successResponse = createSuccessfulGraphQLResult()
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns successResponse

        // When
        val result = sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.VAULT_ID,
            merchantOptInForAppSwitch = false
        )

        // Then
        assertTrue(result is APIResult.Success)
        assertEquals("https://paypal.com/redirect", (result as APIResult.Success).data.launchUrl)
    }

    @Test
    fun `invoke returns failure when GraphQL request fails`() = runTest {
        // Given
        val sdkError = PayPalSDKError(1001, "Test error")
        val failureResponse = GraphQLResult.Failure(sdkError)
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns failureResponse

        // When
        val result = sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true
        )

        // Then
        assertTrue(result is APIResult.Failure)
        assertEquals(sdkError, (result as APIResult.Failure).error)
    }

    @Test
    fun `invoke returns failure when GraphQL response data is null`() = runTest {
        // Given
        val successResponseWithNullData = GraphQLResult.Success(
            data = null,
            correlationId = "correlation-123"
        )
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns successResponseWithNullData

        // When
        val result = sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true
        )

        // Then
        assertTrue(result is APIResult.Failure)
    }

    @Test
    fun `invoke handles malformed JSON response gracefully`() = runTest {
        // Given
        val malformedData = JSONObject().put("invalid", "structure")
        val successResponse = GraphQLResult.Success(
            data = malformedData,
            correlationId = "correlation-123"
        )
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns successResponse

        // When
        val result = sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true
        )

        // Then
        assertTrue(result is APIResult.Failure)
    }

    @Test
    fun `invoke works with different token types`() = runTest {
        // Given
        val successResponse = createSuccessfulGraphQLResult()
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns successResponse

        val requestSlot = slot<JSONObject>()

        // When
        sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.CHECKOUT_TOKEN,
            merchantOptInForAppSwitch = false
        )

        // Then
        coVerify { graphQLClient.send(capture(requestSlot), any(), any()) }
        val variables = requestSlot.captured.getJSONObject("variables")
        assertEquals("CHECKOUT_TOKEN", variables.getString("tokenType"))
        assertEquals(false, variables.getBoolean("merchantOptInForAppSwitch"))
    }

    @Test
    fun `invoke sets experimentation context correctly`() = runTest {
        // Given
        val successResponse = createSuccessfulGraphQLResult()
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Success(
            testToken
        )
        coEvery { graphQLClient.send(any(), any(), any()) } returns successResponse

        val requestSlot = slot<JSONObject>()

        // When
        sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.BILLING_TOKEN,
            merchantOptInForAppSwitch = true
        )

        // Then
        coVerify { graphQLClient.send(capture(requestSlot), any(), any()) }
        val variables = requestSlot.captured.getJSONObject("variables")
        val experimentationContext = variables.getJSONObject("experimentationContext")
        assertEquals("PPCP_NATIVE_SDK", experimentationContext.getString("integrationChannel"))
    }

    @Test
    fun `invoke returns failure when authentication fails`() = runTest {
        // Given
        val sdkError = PayPalSDKError(1001, "Authentication failed")
        coEvery { authenticationSecureTokenServiceAPI.getClientToken() } returns APIResult.Failure(
            sdkError
        )

        // When
        val result = sut.invoke(
            context = context,
            orderId = testOrderId,
            tokenType = TokenType.ORDER_ID,
            merchantOptInForAppSwitch = true
        )

        // Then
        assertTrue(result is APIResult.Failure)
        assertEquals(sdkError, (result as APIResult.Failure).error)
        // Verify GraphQL client was never called since authentication failed
        coVerify(exactly = 0) { graphQLClient.send(any(), any(), any()) }
    }

    private fun createSuccessfulGraphQLResult(): GraphQLResult.Success {
        val responseData = JSONObject().apply {
            put("external", JSONObject().apply {
                put("patchCcoWithAppSwitchEligibility", JSONObject().apply {
                    put("appSwitchEligibility", JSONObject().apply {
                        put("appSwitchEligible", true)
                        put("redirectURL", "https://paypal.com/redirect")
                        put("ineligibleReason", "")
                    })
                })
            })
        }
        return GraphQLResult.Success(
            data = responseData,
            correlationId = "correlation-123"
        )
    }
}