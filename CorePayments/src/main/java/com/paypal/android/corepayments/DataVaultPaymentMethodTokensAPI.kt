package com.paypal.android.corepayments

import android.content.Context
import com.paypal.android.corepayments.graphql.GraphQLClient
import org.json.JSONObject

class DataVaultPaymentMethodTokensAPI internal constructor(
    private val coreConfig: CoreConfig,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {

    constructor(coreConfig: CoreConfig) : this(
        coreConfig,
        GraphQLClient(coreConfig),
        ResourceLoader()
    )

    suspend fun updateSetupTokenForPayPal(context: Context, setupTokenId: String) {
        val query = resourceLoader.loadRawResource(context, R.raw.graphql_query_update_setup_token)

        val experienceContextJSON = JSONObject()
        experienceContextJSON.put("returnUrl", "com.paypal.android.demo://example.com/returnUrl")
        experienceContextJSON.put("cancelUrl", "com.paypal.android.demo://example.com/returnUrl")

        val payPalJSON = JSONObject()
        payPalJSON.put("experienceContext", payPalJSON)

        val paymentSourceJSON = JSONObject()
        paymentSourceJSON.put("paypal", payPalJSON)

        val variables = JSONObject()
            .put("clientId", coreConfig.clientId)
            .put("vaultSetupToken", setupTokenId)
            .put("paymentSource", paymentSourceJSON)

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)
        val graphQLResponse =
            graphQLClient.send(graphQLRequest, queryName = "UpdateVaultSetupToken")
        graphQLResponse.data?.let { responseJSON ->
            val setupToken = responseJSON.getJSONObject("updateVaultSetupToken")
            print(setupToken)
        }
        throw PayPalSDKError(
            0,
            "Error updating setup token: ${graphQLResponse.errors}",
            graphQLResponse.correlationId
        )
    }
}
