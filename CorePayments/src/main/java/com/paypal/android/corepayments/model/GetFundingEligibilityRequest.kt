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
    val clientID: String,
    val merchantID: String,
    val buyerCountry: String? = null,
    val currency: String? = null,
    val intent: String? = null,
    val commit: Boolean? = null,
    val vault: Boolean? = null,
    val disableFunding: List<String>? = null,
    val disableCard: List<String>? = null
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
    val card: CardEligibilityData? = null,
    val venmo: VenmoEligibilityData? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class CardEligibilityData(
    val eligible: Boolean = false,
    val branded: Boolean? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class VenmoEligibilityData(
    val eligible: Boolean = false
)
