package com.paypal.android.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardPaymentToken(
    val id: String,
    val customerId: String,
    @SerialName("last_digits")
    val lastDigits: String,
    val cardBrand: String
)
