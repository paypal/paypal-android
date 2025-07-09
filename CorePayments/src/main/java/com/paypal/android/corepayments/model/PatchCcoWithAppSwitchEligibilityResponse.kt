package com.paypal.android.corepayments.model

data class PatchCcoWithAppSwitchEligibilityResponse(
    val data: ExternalResponse?
) {
    val launchUrl: String?
        get() = data?.external?.patchCcoWithAppSwitchEligibility?.appSwitchEligibility?.redirectURL
}

data class ExternalResponse(
    val external: PatchCcoResponse?
)

data class PatchCcoResponse(
    val patchCcoWithAppSwitchEligibility: AppSwitchEligibilityResponse?
)

data class AppSwitchEligibilityResponse(
    val appSwitchEligibility: AppSwitchEligibility?
)

data class AppSwitchEligibility(
    val appSwitchEligible: Boolean,
    val redirectURL: String?,
    val ineligibleReason: String?
)
