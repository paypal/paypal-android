package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class GetGooglePayConfigResponse(
    val googlePayConfig: GooglePayConfig?
)

@InternalSerializationApi
@Serializable
internal data class GooglePayConfig(
    val apiVersion: Int?,
    val apiVersionMinor: Int?,
    val isEligible: Boolean,
    val merchantInfo: JsonElement?,
    val allowedPaymentMethods: JsonElement?
)
