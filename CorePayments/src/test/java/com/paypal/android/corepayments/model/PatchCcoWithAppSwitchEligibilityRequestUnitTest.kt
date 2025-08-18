package com.paypal.android.corepayments.model

import android.content.Context
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PatchCcoWithAppSwitchEligibilityRequestUnitTest {

    private lateinit var context: Context
    private lateinit var resourceLoader: ResourceLoader

    @Before
    fun beforeEach() {
        context = mockk(relaxed = true)
        resourceLoader = mockk(relaxed = true)
    }

    @Test
    fun `create returns request with GraphQL query when resource loading succeeds`() = runTest {
        // Given
        val testQuery = "mutation UpdateClientConfig { test }"
        val testVariables = Variables(
            tokenType = "ORDER_ID",
            contextId = "test-context",
            token = "test-token",
            merchantOptInForAppSwitch = true,
            paypalNativeAppInstalled = true
        )

        coEvery { resourceLoader.loadRawResource(any(), any()) } returns LoadRawResourceResult.Success(testQuery)

        // When
        val request = PatchCcoWithAppSwitchEligibilityRequest(testVariables)
        val result = request.create(context, resourceLoader)

        // Then
        assertEquals(testQuery, result?.getString("query"))
        assertEquals(
            testVariables.tokenType,
            result?.getJSONObject("variables")?.getString("tokenType")
        )
    }

    @Test
    fun `create returns null when resource loading fails`() = runTest {
        // Given
        val testVariables = Variables(
            tokenType = "VAULT_ID",
            contextId = "test-context",
            token = "test-token",
            merchantOptInForAppSwitch = false,
            paypalNativeAppInstalled = true
        )

        val mockError = PayPalSDKError(0, "File not found")
        coEvery { resourceLoader.loadRawResource(any(), any()) } returns LoadRawResourceResult.Failure(mockError)

        // When
        val request = PatchCcoWithAppSwitchEligibilityRequest(testVariables)
        val result = request.create(context, resourceLoader)

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `Variables data class has correct properties`() {
        // Given
        val variables = Variables(
            experimentationContext = ExperimentationContext(),
            tokenType = "CHECKOUT_TOKEN",
            contextId = "context-123",
            token = "token-456",
            merchantOptInForAppSwitch = true,
            paypalNativeAppInstalled = true
        )

        // Then
        assertEquals("CHECKOUT_TOKEN", variables.tokenType)
        assertEquals("context-123", variables.contextId)
        assertEquals("token-456", variables.token)
        assertEquals(true, variables.merchantOptInForAppSwitch)
        assertEquals("PPCP_NATIVE_SDK", variables.experimentationContext.integrationChannel)
        assertEquals("NATIVE_SDK", variables.experimentationContext.integrationArtifact)
    }

    @Test
    fun `ExperimentationContext has correct default values`() {
        // When
        val experimentationContext = ExperimentationContext()

        // Then
        assertEquals("PPCP_NATIVE_SDK", experimentationContext.integrationChannel)
        assertEquals("NATIVE_SDK", experimentationContext.integrationArtifact)
    }

    @Test
    fun `ExperimentationContext can be created with custom values`() {
        // When
        val experimentationContext = ExperimentationContext(
            integrationChannel = "CUSTOM_CHANNEL",
            integrationArtifact = "CUSTOM_ARTIFACT"
        )

        // Then
        assertEquals("CUSTOM_CHANNEL", experimentationContext.integrationChannel)
        assertEquals("CUSTOM_ARTIFACT", experimentationContext.integrationArtifact)
    }

    @Test
    fun `companion object constants have correct values`() {
        assertEquals("NATIVE_SDK", PatchCcoWithAppSwitchEligibilityRequest.INTEGRATION_ARTIFACT)
        assertEquals("PPCP_NATIVE_SDK", PatchCcoWithAppSwitchEligibilityRequest.INTEGRATION_CHANNEL)
        assertEquals("ANDROID", PatchCcoWithAppSwitchEligibilityRequest.OS_TYPE)
    }

    @Test
    fun `request can be created with empty query and variables`() {
        // Given
        val variables = Variables(
            tokenType = "BILLING_TOKEN",
            contextId = "context",
            token = "token",
            merchantOptInForAppSwitch = false,
            paypalNativeAppInstalled = true
        )

        // When
        val request = PatchCcoWithAppSwitchEligibilityRequest(variables)

        // Then
        assertEquals(variables, request.variables)
    }
}
