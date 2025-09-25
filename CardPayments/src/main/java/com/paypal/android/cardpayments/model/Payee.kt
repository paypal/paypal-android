package com.paypal.android.cardpayments.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
internal data class Payee(
    @SerialName("email_address")
    val emailAddress: String
)
