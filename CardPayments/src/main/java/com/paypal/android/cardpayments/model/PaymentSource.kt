package com.paypal.android.cardpayments.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
internal data class PaymentSource(
    @SerialName("last_digits")
    val lastDigits: String,
    @SerialName("brand")
    val brand: String,
    @SerialName("type")
    val type: String? = null,
    @SerialName("authentication_result")
    val authenticationResult: AuthenticationResult? = null
)
