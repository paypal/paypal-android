package com.paypal.android.api.model.serialization

import kotlinx.serialization.Serializable

// Venmo Setup Request
@Serializable
data class VenmoSetupRequestBody(
    val paymentSource: VenmoSource,
    val customer: VenmoSetupCustomer? = null
)

@Serializable
data class VenmoSource(
    val venmo: VenmoDetails
)

@Serializable
data class VenmoDetails(
    val experienceContext: VenmoVaultExperienceContext,
    val usageType: String,
    val customerType: String? = null,
    val permitMultiplePaymentTokens: Boolean? = null
)

@Serializable
data class VenmoVaultExperienceContext(
    val returnUrl: String,
    val cancelUrl: String,
    val brandName: String? = null,
    val userAction: String? = null,
    val appSwitchContext: VenmoAppSwitchContext
)

@Serializable
data class VenmoSetupCustomer(
    val merchantCustomerId: String
)
