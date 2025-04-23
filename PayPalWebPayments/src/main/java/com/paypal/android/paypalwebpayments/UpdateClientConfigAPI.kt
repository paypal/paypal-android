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
    private val coreConfig: CoreConfig,
    private val applicationContext: Context,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {

    constructor(context: Context, coreConfig: CoreConfig) : this(
        coreConfig,
        context.applicationContext,
        GraphQLClient(coreConfig),
        ResourceLoader()
    )

    suspend fun updateClientConfig(orderId: String): UpdateClientConfigResult {
        @RawRes val resId = R.raw.graphql_query_update_client_config
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success ->
                sendUpdateClientConfigGraphQLRequest(
                    query = result.value,
                    orderId = orderId
                )

            is LoadRawResourceResult.Failure -> UpdateClientConfigResult.Failure(
                PayPalSDKError(0, "TODO: implement")
            )
        }
    }

    private suspend fun sendUpdateClientConfigGraphQLRequest(
        query: String,
        orderId: String
    ): UpdateClientConfigResult {
        val variables = JSONObject()
            .put("orderID", orderId)
            .put("fundingSource", "card")
            .put("integrationArtifact", "MOBILE_SDK")
            .put("userExperienceFlow", "INCONTEXT")
            .put("productFlow", "MOBILE_SDK")
            .put("buttonSessionId", JSONObject.NULL)

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)

        val clientId = coreConfig.clientId
        val graphQLResponse = graphQLClient.send(
            graphQLRequestBody = graphQLRequest,
            queryName = "UpdateClientConfig",
            additionalHeaders = mapOf("paypal-client-context" to clientId)
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
