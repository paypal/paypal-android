package com.paypal.android.api.model.serialization

import kotlinx.serialization.Serializable

// PayPal Setup Request
@Serializable
data class PayPalSetupRequestBody(
    val paymentSource: PayPalSource
)

@Serializable
data class PayPalSource(
    val paypal: PayPalDetails
)

@Serializable
data class PayPalDetails(
    val usageType: String,
    val experienceContext: PayPalExperienceContext
)

@Serializable
data class PayPalExperienceContext(
    val vaultInstruction: String,
    val returnUrl: String,
    val cancelUrl: String
)
