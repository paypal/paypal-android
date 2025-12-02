package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Request data class for PatchCCO with App Switch Eligibility GraphQL operation.
 * Uses Kotlin serialization for JSON handling.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class PatchCcoWithAppSwitchEligibilityVariables(
    val tokenType: String,
    val contextId: String,
    val token: String,
    val merchantOptInForAppSwitch: Boolean,
    val paypalNativeAppInstalled: Boolean,
    val experimentationContext: ExperimentationContext,
    val integrationArtifact: String,
    val userExperienceFlow: String,
    val osType: String
)

/**
 * Experimentation context for the patch CCO request
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class ExperimentationContext(
    val integrationChannel: String,
)

/**
 * Response data class for PatchCCO with App Switch Eligibility GraphQL operation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class PatchCcoWithAppSwitchEligibilityResponse(
    val external: ExternalData? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class ExternalData(
    val patchCcoWithAppSwitchEligibility: PatchCcoData? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class PatchCcoData(
    val appSwitchEligibility: AppSwitchEligibilityData? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class AppSwitchEligibilityData(
    val appSwitchEligible: Boolean = false,
    val redirectURL: String? = null,
    val ineligibleReason: String? = null
)
