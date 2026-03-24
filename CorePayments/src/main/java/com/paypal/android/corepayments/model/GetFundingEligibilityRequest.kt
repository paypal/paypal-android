package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Request data class for GetFundingEligibility GraphQL operation.
 * Uses Kotlin serialization for JSON handling.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class GetFundingEligibilityVariables(
    val merchantID: List<String>,
    val enableFunding: List<String>
)

/**
 * Response data class for GetFundingEligibility GraphQL operation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class GetFundingEligibilityResponse(
    val fundingEligibility: FundingEligibilityData? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class FundingEligibilityData(
    val venmo: VenmoEligibilityData? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class VenmoEligibilityData(
    val eligible: Boolean = false
)
