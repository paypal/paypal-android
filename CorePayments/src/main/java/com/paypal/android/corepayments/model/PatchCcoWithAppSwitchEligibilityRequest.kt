package com.paypal.android.corepayments.model

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader

data class PatchCcoWithAppSwitchEligibilityRequest(
    val query: String = "",
    val variables: Variables
) {
    companion object {
        suspend fun create(context: Context, variables: Variables): PatchCcoWithAppSwitchEligibilityRequest {
            @RawRes val resId = R.raw.graphql_query_patch_cco_app_switch_eligibility
            val resourceLoader = ResourceLoader()

            return when (val result = resourceLoader.loadRawResource(context, resId)) {
                is LoadRawResourceResult.Success ->
                    PatchCcoWithAppSwitchEligibilityRequest(result.value, variables)
                is LoadRawResourceResult.Failure ->
                    PatchCcoWithAppSwitchEligibilityRequest("", variables)
            }
        }
    }
}

data class Variables(
    val experimentationContext: ExperimentationContext,
    val integrationArtifact: String,
    val tokenType: String,
    val userExperienceFlow: String,
    val contextId: String,
    val token: String,
    val osType: String,
    val merchantOptInForAppSwitch: Boolean
)

data class ExperimentationContext(
    val paymentType: String,
    val integrationChannel: String,
    val isWebLLSEligible: Boolean
)
