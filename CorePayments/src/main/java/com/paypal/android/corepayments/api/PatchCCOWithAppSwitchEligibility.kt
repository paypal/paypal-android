package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.TokenType
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.AppSwitchEligibility
import com.paypal.android.corepayments.model.AppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.ExperimentationContext
import com.paypal.android.corepayments.model.ExternalResponse
import com.paypal.android.corepayments.model.PatchCcoResponse
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.Variables
import org.json.JSONObject

class PatchCCOWithAppSwitchEligibility(
    private val coreConfig: CoreConfig,
    private val authenticationSecureTokenServiceAPI: AuthenticationSecureTokenServiceAPI,
    private val graphQLClient: GraphQLClient = GraphQLClient(coreConfig),
) {
    suspend operator fun invoke(
        context: Context,
        orderId: String,
        tokenType: TokenType,
        merchantOptInForAppSwitch: Boolean
    ): APIResult<PatchCcoWithAppSwitchEligibilityResponse> {
        return runCatching {
            val token =
                when (val tokenResult = authenticationSecureTokenServiceAPI.getClientToken()) {
                    is APIResult.Success -> tokenResult.data
                    is APIResult.Failure -> return APIResult.Failure(tokenResult.error)
                }

            val variables = Variables(
                experimentationContext = ExperimentationContext(),
                tokenType = tokenType.name,
                contextId = orderId,
                token = orderId,
                merchantOptInForAppSwitch = merchantOptInForAppSwitch
            )

            val request = PatchCcoWithAppSwitchEligibilityRequest.create(context, variables)

            val variablesJson = createVariablesJson(variables)
            val graphQLRequest = JSONObject()
                .put("query", request.query)
                .put("variables", variablesJson)

            when (val result = graphQLClient.send(
                graphQLRequestBody = graphQLRequest,
                headers = mapOf("Authorization" to "Bearer $token")
            )) {
                is GraphQLResult.Success -> {
                    result.data?.let { data ->
                        parseResponse(data)?.let { response ->
                            APIResult.Success(response)
                        } ?: APIResult.Failure(APIClientError.unknownError())
                    } ?: APIResult.Failure(APIClientError.unknownError())
                }

                is GraphQLResult.Failure -> APIResult.Failure(result.error)
            }
        }.getOrElse { throwable ->
            APIResult.Failure(APIClientError.unknownError(throwable = throwable))
        }
    }

    private fun createVariablesJson(variables: Variables): JSONObject {
        val experimentationContext = JSONObject()
            .put("integrationChannel", variables.experimentationContext.integrationChannel)

        return JSONObject()
            .put("experimentationContext", experimentationContext)
            .put(
                "integrationArtifact",
                PatchCcoWithAppSwitchEligibilityRequest.INTEGRATION_ARTIFACT
            )
            .put("tokenType", variables.tokenType)
            .put("contextId", variables.contextId)
            .put("token", variables.token)
            .put("osType", PatchCcoWithAppSwitchEligibilityRequest.OS_TYPE)
            .put("merchantOptInForAppSwitch", variables.merchantOptInForAppSwitch)
    }

    private fun parseResponse(data: JSONObject): PatchCcoWithAppSwitchEligibilityResponse? =
        runCatching {
            val external = data.optJSONObject("external") ?: return null
            val patchCco = external.optJSONObject("patchCcoWithAppSwitchEligibility") ?: return null
            val appSwitchEligibilityJson = patchCco.optJSONObject("appSwitchEligibility")

            val appSwitchEligibility = appSwitchEligibilityJson?.let {
                AppSwitchEligibility(
                    appSwitchEligible = it.optBoolean("appSwitchEligible"),
                    redirectURL = it.optString("redirectURL"),
                    ineligibleReason = it.optString("ineligibleReason")
                )
            }

            val appSwitchEligibilityResponse = AppSwitchEligibilityResponse(appSwitchEligibility)
            val patchCcoResponse = PatchCcoResponse(appSwitchEligibilityResponse)
            val externalResponse = ExternalResponse(patchCcoResponse)

            PatchCcoWithAppSwitchEligibilityResponse(externalResponse)
        }.getOrThrow()
}