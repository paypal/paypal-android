package com.paypal.android.corepayments

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
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
            is LoadRawResourceResult.Failure -> {
                val error = PayPalSDKError(
                    code = 0,
                    errorDescription = "Failed to load GraphQL query"
                )
                GetGooglePayConfigResult.Failure(error)
            }
        }
    }

    private suspend fun sendGraphQLGooglePayConfigRequest(
        query: String
    ): GetGooglePayConfigResult {
        // TODO: allow this to be provided as a param
        val merchantId = listOf("")
        // TODO: allow this to be provided as a param
        val buyerCountry = "US"
        // TODO: see if we need this
        val merchantOrigin = "com.paypal.android.sdk"

        val variables = GetGooglePayConfigVariables(
            clientId = coreConfig.clientId,
            merchantId = merchantId,
            merchantOrigin = merchantOrigin,
            buyerCountry = buyerCountry
        )

        val graphQLRequest = GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "GetGooglePayConfig"
        )

        val graphQLResponse = graphQLClient.send<
                GetGooglePayConfigResponse,
                GetGooglePayConfigVariables
                >(graphQLRequest)

        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val responseData = graphQLResponse.response.data
                if (responseData == null) {
                    val error = APIClientError.noResponseData(graphQLResponse.correlationId)
                    GetGooglePayConfigResult.Failure(error)
                } else {
                    val configData = responseData.googlePayConfig
                    val config = GooglePayConfig(
                        isEligible = configData.isEligible,
                        allowedPaymentMethods = configData.allowedPaymentMethods,
                        merchantInfo = configData.merchantInfo
                    )
                    GetGooglePayConfigResult.Success(config)
                }
            }

            is GraphQLResult.Failure -> {
                GetGooglePayConfigResult.Failure(graphQLResponse.error)
            }
        }
    }

    suspend fun confirmOrder(
        orderId: String,
        paymentMethodData: GooglePayPaymentMethodData
    ): ApproveGooglePayPaymentResult {
        @RawRes val resId = R.raw.graphql_approve_google_pay_payment
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success ->
                sendGraphQLApproveGooglePayPaymentRequest(result.value, orderId, paymentMethodData)

            is LoadRawResourceResult.Failure -> {
                val error = PayPalSDKError(
                    code = 0,
                    errorDescription = "Failed to load GraphQL mutation"
                )
                ApproveGooglePayPaymentResult.Failure(error)
            }
        }
    }

    private suspend fun sendGraphQLApproveGooglePayPaymentRequest(
        query: String,
        orderId: String,
        paymentMethodData: GooglePayPaymentMethodData
    ): ApproveGooglePayPaymentResult {
        val variables = ApproveGooglePayPaymentVariables(
            paymentMethodData = paymentMethodData,
            clientID = coreConfig.clientId,
            orderID = orderId,
            productFlow = "CUSTOM_DIGITAL_WALLET"
        )

        val graphQLRequest = GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "ApproveGooglePayPayment"
        )

        val graphQLResponse = graphQLClient.send<
                ApproveGooglePayPaymentResponse,
                ApproveGooglePayPaymentVariables
                >(graphQLRequest)

        return when (graphQLResponse) {
            is GraphQLResult.Success -> {
                val responseData = graphQLResponse.response.data
                if (responseData == null) {
                    val error = APIClientError.noResponseData(graphQLResponse.correlationId)
                    ApproveGooglePayPaymentResult.Failure(error)
                } else {
                    ApproveGooglePayPaymentResult.Success(status = responseData.approveGooglePayPayment.status)
                }
            }

            is GraphQLResult.Failure -> {
                ApproveGooglePayPaymentResult.Failure(graphQLResponse.error)
            }
        }
    }
}