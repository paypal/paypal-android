package com.paypal.android.corepayments.api

import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.FundingEligibility
import com.paypal.android.corepayments.model.GetFundingEligibilityResponse
import com.paypal.android.corepayments.model.GetFundingEligibilityVariables
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetFundingEligibility internal constructor(
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader,
    private val coreConfig: CoreConfig,
) {

    constructor(coreConfig: CoreConfig) : this(
        graphQLClient = GraphQLClient(coreConfig),
        resourceLoader = ResourceLoader(),
        coreConfig = coreConfig
    )

    suspend operator fun invoke(
        context: Context,
        clientId: String,
        fundingSource: String,
        buyerCountry: String? = null,
        currency: String? = null
    ): APIResult<FundingEligibility> {
        require(clientId.isNotBlank()) { "Client ID cannot be blank" }
        require(fundingSource.isNotBlank()) { "Funding source cannot be blank" }
        require(coreConfig.merchantId.isNotBlank()) { "Merchant ID cannot be blank" }

        val graphQLRequest = createGraphQLRequest(
            context = context,
            fundingSource = fundingSource,
            merchantId = coreConfig.merchantId
        ) ?: return APIResult.Failure(APIClientError.dataParsingError(correlationId = null))
        return sendGraphQLRequest(graphQLRequest)
    }

    private suspend fun createGraphQLRequest(
        context: Context,
        fundingSource: String,
        merchantId: String
    ): GraphQLRequest<GetFundingEligibilityVariables>? {
        val resourceResult = resourceLoader.loadRawResource(
            context,
            R.raw.graphql_query_get_funding_eligibility
        )

        val query = when (resourceResult) {
            is LoadRawResourceResult.Success -> resourceResult.value
            is LoadRawResourceResult.Failure -> return null
        }

        val variables = GetFundingEligibilityVariables(
            merchantID = listOf(merchantId),
            buyerCountry = "US",
            enableFunding = listOf(fundingSource),
        )

        return GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "GetFundingEligibility"
        )
    }

    private fun parseResponse(response: GetFundingEligibilityResponse): FundingEligibility {
        return FundingEligibility(
            venmoEligible = response.fundingEligibility?.venmo?.eligible ?: false
        )
    }

    private suspend fun sendGraphQLRequest(
        graphQLRequest: GraphQLRequest<GetFundingEligibilityVariables>
    ): APIResult<FundingEligibility> {
        val graphQLResult = graphQLClient.send<
                GetFundingEligibilityResponse,
                GetFundingEligibilityVariables>(
            graphQLRequest
        )
        return when (graphQLResult) {
            is GraphQLResult.Success -> {
                handleSuccessResponse(graphQLResult)
            }

            is GraphQLResult.Failure -> APIResult.Failure(graphQLResult.error)
        }
    }

    private fun handleSuccessResponse(
        graphQLResult: GraphQLResult.Success<GetFundingEligibilityResponse>
    ): APIResult<FundingEligibility> {
        val responseData = graphQLResult.response.data
            ?: return APIResult.Failure(
                APIClientError.noResponseData(graphQLResult.correlationId)
            )

        val fundingEligibility = parseResponse(responseData)
        return APIResult.Success(data = fundingEligibility)
    }
}
