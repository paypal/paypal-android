package com.paypal.android.corepayments

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import org.json.JSONException
import org.json.JSONObject

class GooglePayAPI(
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

    suspend fun getGooglePayConfig(): GetGooglePayConfigResult {
        @RawRes val resId = R.raw.graphql_google_pay_config_sandbox
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success -> sendGraphQLGooglePayConfigRequest(result.value)
            is LoadRawResourceResult.Failure -> TODO("signal error")
        }
    }

    private suspend fun sendGraphQLGooglePayConfigRequest(
        query: String,
    ): GetGooglePayConfigResult {
        // TODO: allow this to be provided as a param
        val merchantId = ""
        // TODO: allow this to be provided as a param
        val buyerCountry = "US"

        // TODO: see if we need this
        val merchantOrigin = ""

        val variables = JSONObject()
            .put("clientId", coreConfig.clientId)
            .put("merchantId", merchantId)
            .put("merchantOrigin", merchantOrigin)
            .put("buyerCountry", buyerCountry)

        val graphQLRequest = JSONObject()
            .put("query", query)
            .put("variables", variables)
        val graphQLResponse =
            graphQLClient.send(graphQLRequest, queryName = "GetGooglePayConfig")
        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val responseJSON = graphQLResponse.data
                if (responseJSON == null) {
                    TODO("handle null response error")
                } else {
                    parseSuccessfulUpdateSuccessJSON(responseJSON, graphQLResponse.correlationId)
                }
            }

            is GraphQLResult.Failure -> {
                TODO("handle graphql failure error")
            }
        }
    }

    private fun parseSuccessfulUpdateSuccessJSON(
        responseBody: JSONObject,
        correlationId: String?
    ): GetGooglePayConfigResult {
        return try {
            val googlePayConfigJSON = responseBody.getJSONObject("googlePayConfig")
            val isEligible = googlePayConfigJSON.getBoolean("isEligible")
            val allowedPaymentMethods =
                googlePayConfigJSON.getJSONArray("allowedPaymentMethods")
            val merchantInfo =
                googlePayConfigJSON.getJSONObject("merchantInfo")
            GetGooglePayConfigResult.Success(
                GooglePayConfig(isEligible, allowedPaymentMethods, merchantInfo)
            )
        } catch (jsonError: JSONException) {
            val message = "Update Setup Token Failed: GraphQL JSON body was invalid."
            val error = PayPalSDKError(0, message, correlationId, reason = jsonError)
            GetGooglePayConfigResult.Failure(error)
        }
    }

    suspend fun confirmOrder(orderId: String, paymentMethodData: JSONObject) {

    }
}