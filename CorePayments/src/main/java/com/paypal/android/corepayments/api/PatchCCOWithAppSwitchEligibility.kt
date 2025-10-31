package com.paypal.android.corepayments.api

import android.content.Context
import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.RestClient
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLRequest
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.ExperimentationContext
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.TokenType
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PatchCCOWithAppSwitchEligibility internal constructor(
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI,
    private val graphQLClient: GraphQLClient,
    private val resourceLoader: ResourceLoader,
) {

    constructor(coreConfig: CoreConfig) : this(
        authenticationSecureTokenServiceAPI = AuthenticationSecureTokenServiceAPI(
            coreConfig,
            RestClient(coreConfig)
        ),
        graphQLClient = GraphQLClient(coreConfig),
        resourceLoader = ResourceLoader()
    )

    suspend operator fun invoke(
        context: Context,
        orderId: String,
        tokenType: TokenType,
        merchantOptInForAppSwitch: Boolean,
        paypalNativeAppInstalled: Boolean
    ): APIResult<AppSwitchEligibility> {
        // TODO: Determine if authentication is needed for new GraphQL client implementation
        // val tokenResult = authenticationSecureTokenServiceAPI.createLowScopedAccessToken()
        // if (tokenResult is APIResult.Failure) {
        //     return APIResult.Failure(tokenResult.error)
        // }
        // val token = (tokenResult as APIResult.Success).data

        return when (val result = resourceLoader.loadRawResource(
            context,
            R.raw.graphql_query_patch_cco_app_switch_eligibility
        )) {
            is LoadRawResourceResult.Success -> {
                val graphQLRequest = createGraphQLRequest(
                    tokenType = tokenType,
                    orderId = orderId,
                    merchantOptInForAppSwitch = merchantOptInForAppSwitch,
                    paypalNativeAppInstalled = paypalNativeAppInstalled,
                    query = result.value
                )

                when (val graphQLResult = graphQLClient.send<PatchCcoWithAppSwitchEligibilityResponse, PatchCcoWithAppSwitchEligibilityRequest>(
                    graphQLRequest = graphQLRequest
                )) {
                    is GraphQLResult.Success -> {
                        graphQLResult.response.data?.let { responseData ->
                            parseResponse(responseData)?.let { appSwitchEligibility ->
                                APIResult.Success(data = appSwitchEligibility)
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

            is LoadRawResourceResult.Failure -> {
                APIResult.Failure(APIClientError.graphQLRequestLoadError())
            }
        }
    }

    private fun createGraphQLRequest(
        tokenType: TokenType,
        orderId: String,
        merchantOptInForAppSwitch: Boolean,
        paypalNativeAppInstalled: Boolean,
        query: String
    ): GraphQLRequest<PatchCcoWithAppSwitchEligibilityRequest> {
        val variables = PatchCcoWithAppSwitchEligibilityRequest(
            tokenType = tokenType.name,
            contextId = orderId,
            token = orderId,
            merchantOptInForAppSwitch = merchantOptInForAppSwitch,
            experimentationContext = ExperimentationContext(
                integrationChannel = INTEGRATION_CHANNEL,
            ),
            integrationArtifact = UpdateClientConfigAPI.Defaults.INTEGRATION_ARTIFACT,
            userExperienceFlow = UpdateClientConfigAPI.Defaults.USER_EXPERIENCE_FLOW,
            osType = OS_TYPE,
            paypalNativeAppInstalled = paypalNativeAppInstalled
        )

        return GraphQLRequest(
            query = query,
            variables = variables,
            operationName = "PatchCcoWithAppSwitchEligibility"
        )
    }

    private fun parseResponse(response: PatchCcoWithAppSwitchEligibilityResponse): AppSwitchEligibility? {
        val appSwitchEligibilityData = response.external
            ?.patchCcoWithAppSwitchEligibility
            ?.appSwitchEligibility

        return appSwitchEligibilityData?.let {
            AppSwitchEligibility(
                appSwitchEligible = it.appSwitchEligible,
                launchUrl = it.redirectURL,
                ineligibleReason = it.ineligibleReason
            )
        }
    }

    companion object{
        const val INTEGRATION_CHANNEL = "PPCP_NATIVE_SDK"
        const val OS_TYPE = "ANDROID"
    }
}
