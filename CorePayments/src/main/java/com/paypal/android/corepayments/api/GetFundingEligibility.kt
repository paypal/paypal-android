package com.paypal.android.corepayments.api

import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.common.Headers
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
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI,
) {

    constructor(coreConfig: CoreConfig) : this(
        graphQLClient = GraphQLClient(coreConfig),
        resourceLoader = ResourceLoader(),
        authenticationSecureTokenServiceAPI = AuthenticationSecureTokenServiceAPI(coreConfig),
    )

    suspend operator fun invoke(
        context: Context,
        clientId: String,
        merchantId: List<String>? = null,
        buyerCountry: String? = null,
        currency: String? = null
    ): APIResult<FundingEligibility> {
        val graphQLRequest = createGraphQLRequest(
            context = context,
            clientId = clientId,
            merchantId = merchantId,
            buyerCountry = buyerCountry,
            currency = currency
        ) ?: return APIResult.Failure(APIClientError.dataParsingError(correlationId = null))
        return sendGraphQLRequestWithLSATAuthentication(graphQLRequest)
    }

    private suspend fun createGraphQLRequest(
        context: Context,
        clientId: String,
        merchantId: List<String>?,
        buyerCountry: String?,
        currency: String?
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
            clientID = clientId,
            merchantID = "V9YP27HFNG2LW",
            buyerCountry = buyerCountry,
            currency = currency
        )

        return GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "GetFundingEligibility"
        )
    }

    private fun parseResponse(response: GetFundingEligibilityResponse): FundingEligibility? {
        val fundingEligibilityData = response.fundingEligibility

        return fundingEligibilityData?.let {
            FundingEligibility(
                cardEligible = it.card?.eligible ?: false,
                venmoEligible = it.venmo?.eligible ?: false
            )
        }
    }

    private suspend fun sendGraphQLRequestWithLSATAuthentication(
        graphQLRequest: GraphQLRequest<GetFundingEligibilityVariables>
    ): APIResult<FundingEligibility> {
        val tokenResult = authenticationSecureTokenServiceAPI.createLowScopedAccessToken()
        if (tokenResult is APIResult.Failure) {
            return APIResult.Failure(tokenResult.error)
        }
        val token = (tokenResult as APIResult.Success).data
        val graphQLResult = graphQLClient.send<
                GetFundingEligibilityResponse,
                GetFundingEligibilityVariables>(
            graphQLRequest,
            additionalHeaders = mapOf(Headers.AUTHORIZATION to "Bearer $token")
        )
        return when (graphQLResult) {
            is GraphQLResult.Success -> {
                graphQLResult.response.data?.let { responseData ->
                    parseResponse(responseData)?.let { fundingEligibility ->
                        APIResult.Success(data = fundingEligibility)
                    } ?: APIResult.Failure(
                        APIClientError.dataParsingError(graphQLResult.correlationId)
                    )
                } ?: APIResult.Failure(
                    APIClientError.noResponseData(graphQLResult.correlationId)
                )
            }

            is GraphQLResult.Failure -> APIResult.Failure(graphQLResult.error)
        }
    }
}
