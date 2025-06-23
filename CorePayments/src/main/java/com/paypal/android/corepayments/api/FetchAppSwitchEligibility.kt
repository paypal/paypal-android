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

public class FetchAppSwitchEligibility(
    private val coreConfig: CoreConfig,
    private val graphQLClient: GraphQLClient = GraphQLClient(coreConfig),
) {
    suspend operator fun invoke(
        context: Context,
        token: String,
        orderId: String,
        fundingSource: String = "paypal",
        integrationArtifact: String = "PAYPAL_JS_SDK",
        productFlow: String = "SMART_PAYMENT_BUTTONS",
        osType: String = "ANDROID",
        merchantOptInForAppSwitch: Boolean = true
    ): PatchCcoWithAppSwitchEligibilityResponse? {
        val experimentationContext = ExperimentationContext(
            merchantCountry = "US",
            isWebView = false,
            paymentType = "COMMIT",
            integrationChannel = "PPCP_JS_SDK",
            isWebLLSEligible = false
        )

        val variables = Variables(
            fundingSource = fundingSource,
            experimentationContext = experimentationContext,
            integrationArtifact = integrationArtifact,
            tokenType = "ORDER_ID",
            userExperienceFlow = "INCONTEXT",
            contextId = orderId,
            productFlow = productFlow,
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
            .put("merchantCountry", variables.experimentationContext.merchantCountry)
            .put("isWebView", variables.experimentationContext.isWebView)
            .put("paymentType", variables.experimentationContext.paymentType)
            .put("integrationChannel", variables.experimentationContext.integrationChannel)
            .put("isWebLLSEligible", variables.experimentationContext.isWebLLSEligible)

        return JSONObject()
            .put("fundingSource", variables.fundingSource)
            .put("experimentationContext", experimentationContext)
            .put("integrationArtifact", variables.integrationArtifact)
            .put("tokenType", variables.tokenType)
            .put("userExperienceFlow", variables.userExperienceFlow)
            .put("contextId", variables.contextId)
            .put("productFlow", variables.productFlow)
            .put("token", variables.token)
            .put("osType", variables.osType)
            .put("merchantOptInForAppSwitch", variables.merchantOptInForAppSwitch)
            .apply {
                variables.buttonSessionID?.let { put("buttonSessionID", it) }
            }
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
