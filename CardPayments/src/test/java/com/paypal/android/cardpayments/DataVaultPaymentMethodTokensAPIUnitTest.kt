package com.paypal.android.cardpayments

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.common.GraphQLClient
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
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

        val card = Card("4111111111111111", "01", "24", "123")
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
}