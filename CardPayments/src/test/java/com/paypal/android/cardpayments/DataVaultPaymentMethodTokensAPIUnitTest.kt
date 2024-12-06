package com.paypal.android.cardpayments

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.Address
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

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

        val requestBodySlot = slot<JSONObject>()
        coVerify { graphQLClient.send(capture(requestBodySlot), "UpdateVaultSetupToken") }
        val actualRequestBody = requestBodySlot.captured

        val expectedQuery = resourceLoader.loadRawResource(
            context,
            R.raw.graphql_query_update_setup_token
        ) as LoadRawResourceResult.Success

        // language=JSON
        val expectedRequestBody = """
        {
            "query": "${expectedQuery.value}",
            "variables": {
                "clientId": "fake-client-id",
                "vaultSetupToken": "fake-setup-token-id",
                "paymentSource": {
                    "card": {
                        "number": "4111111111111111",
                        "expiry": "24-01",
                        "name": "Jane Doe",
                        "securityCode": "123"
                    }
                }
            }
        }
        """

        JSONAssert.assertEquals(JSONObject(expectedRequestBody), actualRequestBody, true)
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

        val requestBodySlot = slot<JSONObject>()
        coVerify { graphQLClient.send(capture(requestBodySlot), "UpdateVaultSetupToken") }
        val actualRequestBody = requestBodySlot.captured

        val expectedQuery = resourceLoader.loadRawResource(
            context,
            R.raw.graphql_query_update_setup_token
        ) as LoadRawResourceResult.Success

        // language=JSON
        val expectedRequestBody = """
        {
            "query": "${expectedQuery.value}",
            "variables": {
                "clientId": "fake-client-id",
                "vaultSetupToken": "fake-setup-token-id",
                "paymentSource": {
                    "card": {
                        "number": "4111111111111111",
                        "expiry": "24-01",
                        "name": "Jane Doe",
                        "securityCode": "123",
                        "billingAddress": {
                            "addressLine1": "2211 N 1st St.",
                            "addressLine2": "Apt. 1A",
                            "adminArea1": "CA",
                            "adminArea2": "San Jose",
                            "postalCode": "95131",
                            "countryCode": "US"
                        }
                    }
                }
            }
        }
        """

        JSONAssert.assertEquals(JSONObject(expectedRequestBody), actualRequestBody, true)
    }

    @Test
    fun updateSetupToken_returnsStatusApprovedVaultResult() = runTest {
        // language=JSON
        val json = """
            {
              "updateVaultSetupToken": {
                "id": "fake-setup-token-id-from-result",
                "status": "APPROVED",
                "links": [
                    { "rel": "self", "href": "https://fake.com/self/url" },
                    { "rel": "confirm", "href": "https://fake.com/confirm/url" }
                ]
              }
            }
        """.trimIndent()
        val graphQLResult = GraphQLResult.Success(JSONObject(json))
        coEvery { graphQLClient.send(any(), "UpdateVaultSetupToken") } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Success

        assertEquals("fake-setup-token-id-from-result", result.setupTokenId)
        assertEquals("APPROVED", result.status)
        assertNull(result.approveHref)
    }

    @Test
    fun updateSetupToken_returnsVaultResultWithPayerActionURL() = runTest {
        // language=JSON
        val json = """
            {
                "updateVaultSetupToken": {
                    "id": "fake-setup-token-id-from-result",
                    "status": "PAYER_ACTION_REQUIRED",
                    "links": [
                        { "rel": "self", "href": "https://fake.com/self/url" },
                        { "rel": "approve", "href": "https://fake.com/approval/url" }
                    ]
                }
            }
        """.trimIndent()
        val graphQLResult = GraphQLResult.Success(JSONObject(json))
        coEvery { graphQLClient.send(any(), "UpdateVaultSetupToken") } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Success

        assertEquals("fake-setup-token-id-from-result", result.setupTokenId)
        assertEquals("PAYER_ACTION_REQUIRED", result.status)
        assertEquals("https://fake.com/approval/url", result.approveHref)
    }

    @Test
    fun updateSetupToken_returnsFailureWhenUpdateVaultSetupTokenFieldIsMissing() = runTest {
        // language=JSON
        val emptyJSON = """{}""".trimIndent()
        val graphQLResult =
            GraphQLResult.Success(JSONObject(emptyJSON), correlationId = "fake-correlation-id")
        coEvery { graphQLClient.send(any(), "UpdateVaultSetupToken") } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Failure

        val expectedMessage = "Update Setup Token Failed: GraphQL JSON body was invalid."
        assertEquals(expectedMessage, result.error.errorDescription)
        assertEquals("fake-correlation-id", result.error.correlationId)
        assertTrue(result.error.cause is JSONException)
    }

    @Test
    fun updateSetupToken_returnsFailureWhenStatusFieldIsMissing() = runTest {
        // language=JSON
        val json = """
            {
              "updateVaultSetupToken": {
                "id": "fake-setup-token-id-from-result"
              }
            }
        """.trimIndent()
        val graphQLResult =
            GraphQLResult.Success(JSONObject(json), correlationId = "fake-correlation-id")
        coEvery { graphQLClient.send(any(), "UpdateVaultSetupToken") } returns graphQLResult

        sut = DataVaultPaymentMethodTokensAPI(coreConfig, context, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken("fake-setup-token-id", card)
                as UpdateSetupTokenResult.Failure

        val expectedMessage = "Update Setup Token Failed: GraphQL JSON body was invalid."
        assertEquals(expectedMessage, result.error.errorDescription)
        assertEquals("fake-correlation-id", result.error.correlationId)
        assertTrue(result.error.cause is JSONException)
    }
}
