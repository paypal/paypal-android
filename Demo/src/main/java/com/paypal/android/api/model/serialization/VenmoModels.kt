package com.paypal.android.api.model.serialization

import kotlinx.serialization.Serializable

// Venmo Setup Request
@Serializable
data class VenmoSetupRequestBody(
    val paymentSource: VenmoSource
)

@Serializable
data class VenmoSource(
    val venmo: VenmoDetails
)

@Serializable
data class VenmoDetails(
    val usageType: String,
    val experienceContext: VenmoVaultExperienceContext
)

@Serializable
data class VenmoVaultExperienceContext(
    val vaultInstruction: String
)
