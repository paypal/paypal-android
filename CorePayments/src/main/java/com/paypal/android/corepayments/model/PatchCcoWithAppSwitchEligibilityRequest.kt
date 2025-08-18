package com.paypal.android.corepayments.model

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest.Companion.INTEGRATION_ARTIFACT
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest.Companion.INTEGRATION_CHANNEL
import org.json.JSONObject

internal data class PatchCcoWithAppSwitchEligibilityRequest(
    val variables: Variables
) {
    suspend fun create(
        context: Context,
        resourceLoader: ResourceLoader = ResourceLoader()
    ): JSONObject? {
        @RawRes val resId = R.raw.graphql_query_patch_cco_app_switch_eligibility
        val query = when (val result = resourceLoader.loadRawResource(context, resId)) {
            is LoadRawResourceResult.Success -> result.value
            is LoadRawResourceResult.Failure -> return null
        }
        val variablesJson = createVariablesJson(variables)
        return JSONObject()
            .put("query", query)
            .put("variables", variablesJson)
    }

    private fun createVariablesJson(variables: Variables): JSONObject {
        val experimentationContext = JSONObject()
            .put("integrationChannel", variables.experimentationContext.integrationChannel)

        return JSONObject()
            .put("experimentationContext", experimentationContext)
            .put(
                "integrationArtifact",
                INTEGRATION_ARTIFACT
            )
            .put("tokenType", variables.tokenType)
            .put("contextId", variables.contextId)
            .put("token", variables.token)
            .put("osType", OS_TYPE)
            .put("merchantOptInForAppSwitch", variables.merchantOptInForAppSwitch)
            .put("paypalNativeAppInstalled", variables.paypalNativeAppInstalled)
    }

    companion object {
        const val INTEGRATION_ARTIFACT =
            "NATIVE_SDK" // TODO: use Mobile SDK artifact after backend changes
        const val INTEGRATION_CHANNEL = "PPCP_NATIVE_SDK"
        const val OS_TYPE = "ANDROID"
    }
}

data class Variables(
    val tokenType: String,
    val contextId: String,
    val token: String,
    val merchantOptInForAppSwitch: Boolean,
    val paypalNativeAppInstalled: Boolean,
    val experimentationContext: ExperimentationContext = ExperimentationContext(),
)

data class ExperimentationContext(
    val integrationChannel: String = INTEGRATION_CHANNEL,
    val integrationArtifact: String = INTEGRATION_ARTIFACT
)
