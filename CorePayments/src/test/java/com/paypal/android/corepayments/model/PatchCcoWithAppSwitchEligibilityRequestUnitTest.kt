package com.paypal.android.corepayments.model

import android.content.Context
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PatchCcoWithAppSwitchEligibilityRequestUnitTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun beforeEach() {
        context = mockk(relaxed = true)
        resources = mockk(relaxed = true)
        every { context.resources } returns resources
    }

    @Test
    fun `create returns request with GraphQL query when resource loading succeeds`() = runTest {
        // Given
        val testQuery = "mutation UpdateClientConfig { test }"
        val testVariables = Variables(
            tokenType = "ORDER_ID",
            contextId = "test-context",
            token = "test-token",
            merchantOptInForAppSwitch = true
        )

        val inputStream = ByteArrayInputStream(testQuery.toByteArray())
        every { resources.openRawResource(any()) } returns inputStream

        // When
        val result = PatchCcoWithAppSwitchEligibilityRequest.create(context, testVariables)

        // Then
        assertEquals(testQuery, result.query)
        assertEquals(testVariables, result.variables)
    }

    @Test
    fun `create returns empty query when resource loading fails`() = runTest {
        // Given
        val testVariables = Variables(
            tokenType = "VAULT_ID",
            contextId = "test-context",
            token = "test-token",
            merchantOptInForAppSwitch = false
        )

        every { resources.openRawResource(any()) } throws Resources.NotFoundException("File not found")

        // When
        val result = PatchCcoWithAppSwitchEligibilityRequest.create(context, testVariables)

        // Then
        assertEquals("", result.query)
        assertEquals(testVariables, result.variables)
    }

    @Test
    fun `Variables data class has correct properties`() {
        // Given
        val variables = Variables(
            experimentationContext = ExperimentationContext(),
            tokenType = "CHECKOUT_TOKEN",
            contextId = "context-123",
            token = "token-456",
            merchantOptInForAppSwitch = true
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
            merchantOptInForAppSwitch = false
        )

        // When
        val request = PatchCcoWithAppSwitchEligibilityRequest(
            query = "",
            variables = variables
        )

        // Then
        assertEquals("", request.query)
        assertEquals(variables, request.variables)
    }
}