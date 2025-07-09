package com.paypal.android.corepayments.model

import com.paypal.android.corepayments.APIClientError
import org.json.JSONObject

data class PatchCcoWithAppSwitchEligibilityResponse(
    val data: ExternalResponse
) {
    companion object {
        fun parse(data: JSONObject, correlationId: String?): PatchCcoResponse {
            val external = data.optJSONObject("external")
                ?: throwParsingError(correlationId)
            val patchCco = external.optJSONObject("patchCcoWithAppSwitchEligibility")
                ?: throwParsingError(correlationId)
            val appSwitchEligibilityJson = patchCco.optJSONObject("appSwitchEligibility")

            val appSwitchEligibility = appSwitchEligibilityJson?.let {
                AppSwitchEligibility(
                    appSwitchEligible = it.optBoolean("appSwitchEligible"),
                    redirectURL = it.optString("redirectURL"),
                    ineligibleReason = it.optString("ineligibleReason")
                )
            } ?: throwParsingError(correlationId)

            val appSwitchEligibilityResponse = AppSwitchEligibilityResponse(appSwitchEligibility)
            val patchCcoResponse = PatchCcoResponse(appSwitchEligibilityResponse)
            return patchCcoResponse
        }

        private fun throwParsingError(correlationId: String?): Nothing {
            throw APIClientError.dataParsingError(correlationId)
        }
    }

    val launchUrl: String?
        get() = data.external.patchCcoWithAppSwitchEligibility.appSwitchEligibility.redirectURL

    val isAppSwitchEligible: Boolean
        get() = data.external.patchCcoWithAppSwitchEligibility.appSwitchEligibility.appSwitchEligible
}

data class ExternalResponse(
    val external: PatchCcoResponse
)

data class PatchCcoResponse(
    val patchCcoWithAppSwitchEligibility: AppSwitchEligibilityResponse
)

data class AppSwitchEligibilityResponse(
    val appSwitchEligibility: AppSwitchEligibility
)

data class AppSwitchEligibility(
    val appSwitchEligible: Boolean,
    val redirectURL: String?,
    val ineligibleReason: String?
)
