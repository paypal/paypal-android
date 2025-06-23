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
    val fundingSource: String,
    val experimentationContext: ExperimentationContext,
    val integrationArtifact: String,
    val tokenType: String,
    val userExperienceFlow: String,
    val contextId: String,
    val productFlow: String,
    val token: String,
    val osType: String,
    val merchantOptInForAppSwitch: Boolean,
    val buttonSessionID: String? = null
)

data class ExperimentationContext(
    val merchantCountry: String,
    val isWebView: Boolean,
    val paymentType: String,
    val integrationChannel: String,
    val isWebLLSEligible: Boolean
)
