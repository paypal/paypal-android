package com.paypal.android.cardpayments

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.cardpayments.api.LinkData
import com.paypal.android.cardpayments.api.SetupTokenData
import com.paypal.android.cardpayments.api.UpdateSetupTokenResponse
import com.paypal.android.cardpayments.api.UpdateSetupTokenVariables
import com.paypal.android.corepayments.Address
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResponse
import com.paypal.android.corepayments.graphql.GraphQLResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(InternalSerializationApi::class)
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DataVaultPaymentMethodTokensAPIUnitTest {

    private val coreConfig = CoreConfig("fake-client-id", Environment.SANDBOX)

    private val resourceLoader = ResourceLoader()
    private val context = ApplicationProvider.getApplicationContext<Application>()

    private lateinit var card: Card
    private lateinit var graphQLClient: GraphQLClient

    private lateinit var sut: DataVaultPaymentMethodTokensAPI

    @Before
    fun beforeEach() {
        card = Card(
            number = "4111111111111111",
            expirationMonth = "01",
            expirationYear = "24",
            securityCode = "123",
            cardholderName = "Jane Doe",
        )
        graphQLClient = mockk(relaxed = true)
    }

    @Test
    fun updateSetupToken_forCardWithRequiredFieldsSet_sendsGraphQLRequest() = runTest {
        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        sut.updateSetupToken("fake-setup-token-id", card)

        val requestBodySlot = slot<GraphQLRequest<UpdateSetupTokenVariables>>()
        coVerify {
            graphQLClient.send<UpdateSetupTokenResponse, UpdateSetupTokenVariables>(
                capture(requestBodySlot)
            )
        }
        val request = requestBodySlot.captured

        val expectedQuery = resourceLoader.loadRawResource(
            context,
            R.raw.graphql_query_update_setup_token
        ) as LoadRawResourceResult.Success

        assertEquals(expectedQuery.value, request.query)

        // Verify variables
        val variables = request.variables!!
        assertEquals("fake-client-id", variables.clientId)
        assertEquals("fake-setup-token-id", variables.vaultSetupToken)

        // Verify card details
        val card = variables.paymentSource.card
        assertEquals("4111111111111111", card.number)
        assertEquals("24-01", card.expiry)
        assertEquals("Jane Doe", card.name)
        assertEquals("123", card.securityCode)
        assertNull(card.billingAddress)
    }

    @Test
    fun updateSetupToken_forCardWithRequiredAndOptionalFieldsSet_sendsGraphQLRequest() = runTest {
        card.billingAddress = Address(
            streetAddress = "2211 N 1st St.",
            extendedAddress = "Apt. 1A",
            locality = "San Jose",
            region = "CA",
            postalCode = "95131",
            countryCode = "US"
        )

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        sut.updateSetupToken("fake-setup-token-id", card)

        val requestBodySlot = slot<GraphQLRequest<UpdateSetupTokenVariables>>()
        coVerify {
            graphQLClient.send<UpdateSetupTokenResponse, UpdateSetupTokenVariables>(
                capture(requestBodySlot)
            )
        }
        val request = requestBodySlot.captured

        val expectedQuery = resourceLoader.loadRawResource(
            context,
            R.raw.graphql_query_update_setup_token
        ) as LoadRawResourceResult.Success

        assertEquals(expectedQuery.value, request.query)

        // Verify variables
        val variables = request.variables!!
        assertEquals("fake-client-id", variables.clientId)
        assertEquals("fake-setup-token-id", variables.vaultSetupToken)

        // Verify card details
        val card = variables.paymentSource.card
        assertEquals(card, variables.paymentSource.card)
        assertEquals("4111111111111111", card.number)
        assertEquals("24-01", card.expiry)
        assertEquals("Jane Doe", card.name)
        assertEquals("123", card.securityCode)

        // Verify billing address
        val billingAddress = card.billingAddress
        assertNotNull(billingAddress)
        assertEquals("2211 N 1st St.", billingAddress?.addressLine1)
        assertEquals("Apt. 1A", billingAddress?.addressLine2)
        assertEquals("CA", billingAddress?.adminArea1)
        assertEquals("San Jose", billingAddress?.adminArea2)
        assertEquals("95131", billingAddress?.postalCode)
        assertEquals("US", billingAddress?.countryCode)
    }

    @Test
    fun updateSetupToken_returnsStatusApprovedVaultResult() = runTest {
        val setupTokenData = SetupTokenData(
            id = "fake-setup-token-id-from-result",
            status = "APPROVED",
            links = listOf(
                LinkData(rel = "self", href = "https://fake.com/self/url"),
                LinkData(rel = "confirm", href = "https://fake.com/confirm/url")
            )
        )
        val responseData = UpdateSetupTokenResponse(updateVaultSetupToken = setupTokenData)
        val graphQLResponse = GraphQLResponse(responseData)
        val graphQLResult = GraphQLResult.Success<UpdateSetupTokenResponse>(graphQLResponse)
        coEvery {
            graphQLClient.send<UpdateSetupTokenResponse, UpdateSetupTokenVariables>(
                any()
            )
        } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Success

        assertEquals("fake-setup-token-id-from-result", result.setupTokenId)
        assertEquals("APPROVED", result.status)
        assertNull(result.approveHref)
    }

    @Test
    fun updateSetupToken_returnsVaultResultWithPayerActionURL() = runTest {
        val setupTokenData = SetupTokenData(
            id = "fake-setup-token-id-from-result",
            status = "PAYER_ACTION_REQUIRED",
            links = listOf(
                LinkData(rel = "self", href = "https://fake.com/self/url"),
                LinkData(rel = "approve", href = "https://fake.com/approval/url")
            )
        )
        val responseData = UpdateSetupTokenResponse(updateVaultSetupToken = setupTokenData)
        val graphQLResponse = GraphQLResponse(responseData)
        val graphQLResult = GraphQLResult.Success<UpdateSetupTokenResponse>(graphQLResponse)
        coEvery {
            graphQLClient.send<UpdateSetupTokenResponse, UpdateSetupTokenVariables>(
                any()
            )
        } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Success

        assertEquals("fake-setup-token-id-from-result", result.setupTokenId)
        assertEquals("PAYER_ACTION_REQUIRED", result.status)
        assertEquals("https://fake.com/approval/url", result.approveHref)
    }

    @Test
    fun updateSetupToken_returnsFailureWhenUpdateVaultSetupTokenFieldIsMissing() = runTest {
        val graphQLResult = GraphQLResult.Failure(
            PayPalSDKError(0, "Missing required field", "fake-correlation-id")
        )
        coEvery {
            graphQLClient.send<UpdateSetupTokenResponse, UpdateSetupTokenVariables>(
                any()
            )
        } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Failure

        val expectedMessage = "Missing required field"
        assertEquals(expectedMessage, result.error.errorDescription)
        assertEquals("fake-correlation-id", result.error.correlationId)
    }

    @Test
    fun updateSetupToken_returnsFailureWhenStatusFieldIsMissing() = runTest {
        val graphQLResult = GraphQLResult.Failure(
            PayPalSDKError(0, "Serialization failed", "fake-correlation-id")
        )
        coEvery {
            graphQLClient.send<UpdateSetupTokenResponse, UpdateSetupTokenVariables>(
                any()
            )
        } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Failure

        val expectedMessage = "Serialization failed"
        assertEquals(expectedMessage, result.error.errorDescription)
        assertEquals("fake-correlation-id", result.error.correlationId)
    }
}
