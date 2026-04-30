package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
data class GetGooglePayConfigResponse(
    val googlePayConfig: GooglePayConfig?
)

@InternalSerializationApi
@Serializable
data class GooglePayConfig(
    val apiVersion: Int?,
    val apiVersionMinor: Int?,
    val isEligible: Boolean,
    val merchantInfo: GooglePayMerchantInfo?,
    val allowedPaymentMethods: List<GooglePayPaymentMethod>?
)

@InternalSerializationApi
@Serializable
data class GooglePayMerchantInfo(
    val merchantName: String?,
    val merchantId: String,
    val merchantOrigin: String,
    val authJwt: String
)

@InternalSerializationApi
@Serializable
data class GooglePayPaymentMethod(val type: String)

