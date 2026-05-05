package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class GooglePayPaymentDataRequest(
    val apiVersion: Int,
    val apiVersionMinor: Int,
    val merchantInfo: JsonElement?,
    val allowedPaymentMethods: JsonElement?,
    val callbackIntents: List<String>,
    val transactionInfo: GooglePayTransactionInfo
)

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class GooglePayTransactionInfo(
    val countryCode: String,
    val currencyCode: String,
    val totalPriceStatus: String,
    val totalPrice: String,
    val totalPriceLabel: String,
    val displayItems: List<GooglePayTransactionDisplayItem>
)

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class GooglePayTransactionDisplayItem(
    val label: String,
    val type: String,
    val price: String
)
