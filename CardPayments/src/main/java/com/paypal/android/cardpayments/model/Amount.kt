package com.paypal.android.cardpayments.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
internal data class Amount(
    @SerialName("currency_code")
    val currencyCode: String?,
    @SerialName("value")
    val value: String?
)
