package com.paypal.android.corepayments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetGooglePayConfigVariables(
    @SerialName("clientId")
    val clientId: String,
    @SerialName("merchantId")
    val merchantId: List<String>,
    @SerialName("merchantOrigin")
    val merchantOrigin: String,
    @SerialName("buyerCountry")
    val buyerCountry: String
)
