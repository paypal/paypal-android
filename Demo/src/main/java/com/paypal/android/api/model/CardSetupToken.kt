package com.paypal.android.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardSetupToken(
    val id: String,
    val customerId: String,
    val status: String,
    @SerialName("verification_status")
    val verificationStatus: String? = null,
)
