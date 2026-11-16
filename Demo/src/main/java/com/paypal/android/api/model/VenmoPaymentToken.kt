package com.paypal.android.api.model

import kotlinx.serialization.Serializable

@Serializable
data class VenmoPaymentToken(
    val id: String,
    val customerId: String,
)
