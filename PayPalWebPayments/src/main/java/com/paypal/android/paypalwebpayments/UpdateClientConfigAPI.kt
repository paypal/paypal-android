package com.paypal.android.paypalwebpayments

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient

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

    suspend fun updateClientConfig(): UpdateClientConfigResult {
        @RawRes val resId = R.raw.graphql_query_update_client_config
        return when (val result = resourceLoader.loadRawResource(applicationContext, resId)) {
            is LoadRawResourceResult.Success ->
                sendUpdateClientConfigGraphQLRequest(result.value)

            is LoadRawResourceResult.Failure -> UpdateClientConfigResult.Failure(
                PayPalSDKError(123, "TODO: implement")
            )
        }
    }

    private suspend fun sendUpdateClientConfigGraphQLRequest(
        query: String,
    ): UpdateClientConfigResult {
        return UpdateClientConfigResult.Failure(PayPalSDKError(123, "TODO: implement"))
    }
}