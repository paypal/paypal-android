package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
data class GetGooglePayConfigResponse(
    val googlePayConfig: GooglePayConfig?
)

@InternalSerializationApi
@Serializable
data class GooglePayConfig(
    val isEligible: Boolean,
    val allowedPaymentMethods: JsonElement,
    val merchantInfo: JsonElement
)
