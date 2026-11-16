package com.paypal.android.api.model

import kotlinx.serialization.Serializable

@Serializable
data class VenmoSetupToken(
    val id: String,
    val customerId: String,
    val status: String
)
