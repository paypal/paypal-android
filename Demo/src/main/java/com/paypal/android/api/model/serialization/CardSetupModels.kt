package com.paypal.android.api.model.serialization

import kotlinx.serialization.Serializable

@Serializable
data class CardSetupRequest(
    val paymentSource: CardPaymentSource
)

@Serializable
data class CardPaymentSource(
    val card: CardDetails
)

@Serializable
data class CardDetails(
    val verificationMethod: String,
    val experienceContext: ExperienceContext
)

@Serializable
data class ExperienceContext(
    val returnUrl: String,
    val cancelUrl: String
)
