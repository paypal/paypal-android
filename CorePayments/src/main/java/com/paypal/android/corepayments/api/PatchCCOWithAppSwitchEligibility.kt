package com.paypal.android.corepayments.api

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.graphql.GraphQLClient
import com.paypal.android.corepayments.graphql.GraphQLResult
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
    private val graphQLClient: GraphQLClient = GraphQLClient(coreConfig),
) {
    suspend operator fun invoke(
        context: Context,
        token: String,
        orderId: String,
        tokenType: String = "ORDER_ID",
        integrationArtifact: String = "NATIVE_SDK",
        osType: String = "ANDROID",
        merchantOptInForAppSwitch: Boolean = true
    ): PatchCcoWithAppSwitchEligibilityResponse? {
        val experimentationContext = ExperimentationContext(
            paymentType = "CONTINUE",
            integrationChannel = "PPCP_NATIVE_SDK",
            isWebLLSEligible = true
        )

        val variables = Variables(
            experimentationContext = experimentationContext,
            integrationArtifact = integrationArtifact,
            tokenType = tokenType,
            userExperienceFlow = "INCONTEXT",
            contextId = orderId,
            token = orderId,
            osType = osType,
            merchantOptInForAppSwitch = merchantOptInForAppSwitch
        )

        val request = PatchCcoWithAppSwitchEligibilityRequest.create(context, variables)

        val variablesJson = createVariablesJson(variables)
        val graphQLRequest = JSONObject()
            .put("query", request.query)
            .put("variables", variablesJson)

        return when (val result = graphQLClient.send(
            graphQLRequestBody = graphQLRequest,
            headers = mapOf("Authorization" to "Bearer $token")
        )) {
            is GraphQLResult.Success -> {
                result.data?.let { data ->
                    parseResponse(data)
                }
            }
            is GraphQLResult.Failure -> null
        }
    }

    private fun createVariablesJson(variables: Variables): JSONObject {
        val experimentationContext = JSONObject()
            .put("paymentType", variables.experimentationContext.paymentType)
            .put("integrationChannel", variables.experimentationContext.integrationChannel)
            .put("isWebLLSEligible", variables.experimentationContext.isWebLLSEligible)

        return JSONObject()
            .put("experimentationContext", experimentationContext)
            .put("integrationArtifact", variables.integrationArtifact)
            .put("tokenType", variables.tokenType)
            .put("userExperienceFlow", variables.userExperienceFlow)
            .put("contextId", variables.contextId)
            .put("token", variables.token)
            .put("osType", variables.osType)
            .put("merchantOptInForAppSwitch", variables.merchantOptInForAppSwitch)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun parseResponse(data: JSONObject): PatchCcoWithAppSwitchEligibilityResponse? {
        return try {
            val external = data.optJSONObject("external")
            val patchCco = external?.optJSONObject("patchCcoWithAppSwitchEligibility")
            val appSwitchEligibilityJson = patchCco?.optJSONObject("appSwitchEligibility")

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
        } catch (e: Exception) {
            null
        }
    }
}
