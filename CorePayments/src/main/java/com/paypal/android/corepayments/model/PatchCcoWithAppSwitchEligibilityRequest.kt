package com.paypal.android.corepayments.model

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.LoadRawResourceResult
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.ResourceLoader
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest.Companion.INTEGRATION_ARTIFACT
import com.paypal.android.corepayments.model.PatchCcoWithAppSwitchEligibilityRequest.Companion.INTEGRATION_CHANNEL

data class PatchCcoWithAppSwitchEligibilityRequest(
    val query: String = "",
    val variables: Variables
) {
    companion object {
        const val INTEGRATION_ARTIFACT = "NATIVE_SDK"
        const val INTEGRATION_CHANNEL = "PPCP_NATIVE_SDK"
        const val OS_TYPE = "ANDROID"

        suspend fun create(
            context: Context,
            variables: Variables,
            resourceLoader: ResourceLoader = ResourceLoader()
        ): PatchCcoWithAppSwitchEligibilityRequest {
            @RawRes val resId = R.raw.graphql_query_patch_cco_app_switch_eligibility

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
    val tokenType: String,
    val contextId: String,
    val token: String,
    val merchantOptInForAppSwitch: Boolean,
    val experimentationContext: ExperimentationContext = ExperimentationContext(),
)

data class ExperimentationContext(
    val integrationChannel: String = INTEGRATION_CHANNEL,
    val integrationArtifact: String = INTEGRATION_ARTIFACT
)
