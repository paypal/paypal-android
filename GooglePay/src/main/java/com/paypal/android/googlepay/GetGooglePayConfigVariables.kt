package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
data class GetGooglePayConfigVariables(
    val clientId: String,
    val merchantId: List<String>,
    val merchantOrigin: String,
)