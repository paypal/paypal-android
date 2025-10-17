package com.paypal.android.paypalwebpayments

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import org.json.JSONException
import org.json.JSONObject

internal class UpdateClientConfigAPI(
    private val applicationContext: Context,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {

    constructor(context: Context, coreConfig: CoreConfig) : this(
        context.applicationContext,
        GraphQLClient(coreConfig),
        ResourceLoader()
    )

    private object Defaults {
        const val INTEGRATION_ARTIFACT = "MOBILE_SDK"
        const val USER_EXPERIENCE_FLOW = "INCONTEXT"
        const val PRODUCT_FLOW = "MOBILE_SDK"
    }

    suspend fun updateClientConfig(
        orderId: String,
        fundingSource: PayPalWebCheckoutFundingSource
    ): UpdateClientConfigResult {
        @RawRes val resId = R.raw.graphql_query_update_client_config
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success ->
                sendUpdateClientConfigGraphQLRequest(
                    query = result.value,
                    orderId = orderId,
                    fundingSource = fundingSource
                )

            is LoadRawResourceResult.Failure -> UpdateClientConfigResult.Failure(
                PayPalSDKError(0, "TODO: implement")
            )
        }
    }

    private suspend fun sendUpdateClientConfigGraphQLRequest(
        query: String,
        orderId: String,
        fundingSource: PayPalWebCheckoutFundingSource
    ): UpdateClientConfigResult {
        val variables = JSONObject()
            .put("orderID", orderId)
            .put("fundingSource", fundingSource.value)
            .put("integrationArtifact", Defaults.INTEGRATION_ARTIFACT)
            .put("userExperienceFlow", Defaults.USER_EXPERIENCE_FLOW)
            .put("productFlow", Defaults.PRODUCT_FLOW)
            .put("buttonSessionId", JSONObject.NULL)

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)

        val graphQLResponse = graphQLClient.send(
            graphQLRequestBody = graphQLRequest,
            queryName = "UpdateClientConfig"
        )
        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val responseJSON = graphQLResponse.data
                if (responseJSON == null) {
                    val error = graphQLResponse.run {
                        val errorDescription = "Error updating client config: $errors"
                        PayPalSDKError(0, errorDescription, correlationId)
                    }
                    UpdateClientConfigResult.Failure(error)
                } else {
                    parseSuccessfulUpdateSuccessJSON(responseJSON, graphQLResponse.correlationId)
                }
            }

            is GraphQLResult.Failure -> UpdateClientConfigResult.Failure(graphQLResponse.error)
        }
    }

    private fun parseSuccessfulUpdateSuccessJSON(
        responseBody: JSONObject,
        correlationId: String?
    ): UpdateClientConfigResult {
        return try {
            val clientConfig = responseBody.optString("updateClientConfig", "")
            UpdateClientConfigResult.Success(clientConfig)
        } catch (jsonError: JSONException) {
            val message = "Update Client Config Failed: GraphQL JSON body was invalid."
            val error = PayPalSDKError(0, message, correlationId, reason = jsonError)
            UpdateClientConfigResult.Failure(error)
        }
    }
}