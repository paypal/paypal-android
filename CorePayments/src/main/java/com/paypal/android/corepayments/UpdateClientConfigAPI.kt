package com.paypal.android.corepayments

import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UpdateClientConfigAPI(
    private val applicationContext: Context,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader
) {

    constructor(context: Context, coreConfig: CoreConfig) : this(
        context.applicationContext,
        GraphQLClient(coreConfig),
        ResourceLoader()
    )

    object Defaults {
        const val INTEGRATION_ARTIFACT = "MOBILE_SDK"
        const val USER_EXPERIENCE_FLOW = "INCONTEXT"
        const val PRODUCT_FLOW = "HERMES"
    }

    suspend fun updateClientConfig(
        tokenId: String,
        fundingSource: String
    ): UpdateClientConfigResult {
        return when (val result = resourceLoader.loadRawResource(
            applicationContext,
            R.raw.graphql_query_update_client_config
        )) {
            is LoadRawResourceResult.Success -> {
                val graphQLRequest = createUpdateClientConfigRequest(
                    token = tokenId,
                    fundingSource = fundingSource,
                    query = result.value
                )

                val graphQLResponse =
                    graphQLClient.send<UpdateClientConfigResponse, UpdateClientConfigVariables>(
                        graphQLRequest = graphQLRequest
                    )
                when (graphQLResponse) {
                    is GraphQLResult.Success -> {
                        val correlationId = graphQLResponse.correlationId
                        graphQLResponse.response.data?.let {
                            UpdateClientConfigResult.Success
                        } ?: UpdateClientConfigResult.Failure(
                            APIClientError.noResponseData(correlationId)
                        )
                    }

                    is GraphQLResult.Failure -> {
                        UpdateClientConfigResult.Failure(graphQLResponse.error)
                    }
                }
            }

            is LoadRawResourceResult.Failure -> {
                UpdateClientConfigResult.Failure(
                    PayPalSDKError(0, "Failed to load GraphQL query resource")
                )
            }
        }
    }

    private fun createUpdateClientConfigRequest(
        token: String,
        fundingSource: String,
        query: String
    ): GraphQLRequest<UpdateClientConfigVariables> {
        val variables = UpdateClientConfigVariables(
            token = token,
            fundingSource = fundingSource,
            integrationArtifact = Defaults.INTEGRATION_ARTIFACT,
            userExperienceFlow = Defaults.USER_EXPERIENCE_FLOW,
            productFlow = Defaults.PRODUCT_FLOW,
            buttonSessionId = null
        )

        val graphQLRequest = GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "UpdateClientConfig"
        )
        return graphQLRequest
    }
}

@Serializable
@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class UpdateClientConfigVariables(
    val token: String,
    val fundingSource: String,
    val integrationArtifact: String,
    val userExperienceFlow: String,
    val productFlow: String,
    val buttonSessionId: String? = null
)

@Serializable
@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class UpdateClientConfigResponse(
    val updateClientConfig: String
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class UpdateClientConfigResult {
    data object Success : UpdateClientConfigResult()
    data class Failure(val error: PayPalSDKError) : UpdateClientConfigResult()
}
