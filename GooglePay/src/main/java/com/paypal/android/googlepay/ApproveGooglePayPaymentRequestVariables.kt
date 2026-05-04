package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class ApproveGooglePayPaymentRequestVariables(
    val paymentMethodData: JsonElement,
    val clientID: String,
    val orderID: String,
)
