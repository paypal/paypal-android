package com.paypal.android.googlepay

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal class GooglePayAPI(
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

    @OptIn(InternalSerializationApi::class)
    suspend fun getGooglePayConfig(merchantId: String): SDKResult<GooglePayConfig> {
        @RawRes val resId = R.raw.graphql_google_pay_config_sandbox
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success ->
                sendGraphQLGooglePayConfigRequest(result.value, merchantId)

            is LoadRawResourceResult.Failure -> TODO("signal error")
        }
    }

    @OptIn(InternalSerializationApi::class)
    private suspend fun sendGraphQLGooglePayConfigRequest(
        query: String,
        merchantId: String
    ): SDKResult<GooglePayConfig> {
        val variables = GetGooglePayConfigVariables(
            clientId = coreConfig.clientId,
            merchantId = listOf(merchantId),
            // TODO: see if we need this
            merchantOrigin = "com.paypal.android.sdk",
        )

        val graphQLRequest = GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "GetGooglePayConfig"
        )
        val graphQLResponse =
            graphQLClient.send<GetGooglePayConfigResponse, GetGooglePayConfigVariables>(
                graphQLRequest
            )
        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val responseData = graphQLResponse.response.data
                if (responseData == null) {
                    TODO("handle null response error")
                } else {
                    val googlePayConfig = responseData.googlePayConfig
                    if (googlePayConfig == null) {
                        TODO("handle error")
                    } else {
                        SDKResult.Success(googlePayConfig)
                    }
                }
            }

            is GraphQLResult.Failure -> {
                print(graphQLResponse)
                TODO("handle graphql failure error")
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun confirmOrder(
        orderId: String,
        paymentMethodData: JSONObject
    ): SDKResult<ApproveGooglePayPayment> {
        @RawRes val resId = R.raw.graphql_approve_google_pay_payment
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success ->
                sendGraphQLApproveGooglePayPaymentRequest(result.value, orderId, paymentMethodData)

            is LoadRawResourceResult.Failure -> TODO("signal error")
        }
    }

    @OptIn(InternalSerializationApi::class)
    private suspend fun sendGraphQLApproveGooglePayPaymentRequest(
        query: String,
        orderId: String,
        paymentMethodData: JSONObject
    ): SDKResult<ApproveGooglePayPayment> {
        val variables = ApproveGooglePayPaymentRequestVariables(
            paymentMethodData = Json.parseToJsonElement(paymentMethodData.toString()),
            clientID = coreConfig.clientId,
            orderID = orderId,
        )
        val graphQLRequest = GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "ApproveGooglePayPayment"
        )
        val graphQLResponse = graphQLClient.send<
                ApproveGooglePayPaymentRequestResponse,
                ApproveGooglePayPaymentRequestVariables
                >(
            graphQLRequest
        )

        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val responseData = graphQLResponse.response.data
                if (responseData == null) {
                    TODO("handle null response error")
                } else {
                    SDKResult.Success(value = responseData.approveGooglePayPayment)
                }
            }

            is GraphQLResult.Failure -> {
                TODO("handle graphql failure error")
            }
        }
    }
}