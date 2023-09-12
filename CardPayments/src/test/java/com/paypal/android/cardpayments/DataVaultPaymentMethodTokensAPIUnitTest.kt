package com.paypal.android.cardpayments

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.Address
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
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

    private lateinit var graphQLClient: GraphQLClient
    private lateinit var sut: DataVaultPaymentMethodTokensAPI

    @Before
    fun beforeEach() {
        graphQLClient = mockk(relaxed = true)
    }

    @Test
    fun updateSetupToken_forCardWithRequiredFieldsSet_sendsGraphQLRequest() = runTest {
        sut = DataVaultPaymentMethodTokensAPI(coreConfig, graphQLClient, resourceLoader)

        val card = Card(
            number = "4111111111111111",
            expirationMonth = "01",
            expirationYear = "24",
            securityCode = "123"
        )
        sut.updateSetupToken(context, "fake-setup-token-id", card)

        val requestBodySlot = slot<JSONObject>()
        coVerify { graphQLClient.send(capture(requestBodySlot), "UpdateVaultSetupToken") }
        val actualRequestBody = requestBodySlot.captured

        val expectedQuery =
            resourceLoader.loadRawResource(context, R.raw.graphql_query_update_setup_token)
        // language=JSON
        val expectedRequestBody = """
        {
            "query": "$expectedQuery",
            "variables": {
                "clientId": "fake-client-id",
                "vaultSetupToken": "fake-setup-token-id",
                "paymentSource": {
                    "card": {
                        "number": "4111111111111111",
                        "expiry": "24-01",
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
        sut = DataVaultPaymentMethodTokensAPI(coreConfig, graphQLClient, resourceLoader)

        val card = Card(
            number = "4111111111111111",
            expirationMonth = "01",
            expirationYear = "24",
            securityCode = "123",
            cardholderName = "Jane Doe",
            billingAddress = Address(
                streetAddress = "2211 N 1st St.",
                extendedAddress = "Apt. 1A",
                locality = "San Jose",
                region = "CA",
                postalCode = "95131",
                countryCode = "US"
            )
        )

        sut.updateSetupToken(context, "fake-setup-token-id", card)

        val requestBodySlot = slot<JSONObject>()
        coVerify { graphQLClient.send(capture(requestBodySlot), "UpdateVaultSetupToken") }
        val actualRequestBody = requestBodySlot.captured

        val expectedQuery =
            resourceLoader.loadRawResource(context, R.raw.graphql_query_update_setup_token)
        // language=JSON
        val expectedRequestBody = """
        {
            "query": "$expectedQuery",
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
    fun updateSetupToken_returnsVaultResult() = runTest {
        // language=JSON
        val json = """
            {
              "updateVaultSetupToken": {
                "id": "fake-setup-token-id-from-result",
                "status": "fake-status"
              }
            }
        """.trimIndent()
        val graphQLResponse = GraphQLResponse(JSONObject(json))
        coEvery { graphQLClient.send(any(), "UpdateVaultSetupToken") } returns graphQLResponse

        val card = Card(
            number = "4111111111111111",
            expirationMonth = "01",
            expirationYear = "24",
            securityCode = "123"
        )
        sut = DataVaultPaymentMethodTokensAPI(coreConfig, graphQLClient, resourceLoader)
        val result = sut.updateSetupToken(context, "fake-setup-token-id", card)

        assertEquals("fake-setup-token-id-from-result", result.setupTokenId)
        assertEquals("fake-status", result.status)
    }
}
