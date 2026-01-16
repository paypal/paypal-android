package com.paypal.android.corepayments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ApproveGooglePayPaymentVariables(
    @SerialName("paymentMethodData")
    val paymentMethodData: GooglePayPaymentMethodData,
    @SerialName("orderID")
    val orderID: String,
    @SerialName("clientID")
    val clientID: String,
    @SerialName("productFlow")
    val productFlow: String,
    @SerialName("shippingAddress")
    val shippingAddress: ShippingAddress? = null,
    @SerialName("email")
    val email: String? = null
)

@Serializable
data class GooglePayPaymentMethodData(
    val type: String,
    val description: String,
    val info: GooglePaymentInfo,
    val tokenizationData: TokenizationData
)

@Serializable
data class GooglePaymentInfo(
    val cardDetails: String? = null,
    val cardNetwork: String? = null,
    val billingAddress: BillingAddress? = null
)

@Serializable
data class TokenizationData(
    val type: String,
    val token: String
)

@Serializable
data class BillingAddress(
    val name: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null,
    val phoneNumber: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val address3: String? = null,
    val locality: String? = null,
    val administrativeArea: String? = null,
    val sortingCode: String? = null
)

@Serializable
data class ShippingAddress(
    val name: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null,
    val phoneNumber: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val address3: String? = null,
    val locality: String? = null,
    val administrativeArea: String? = null,
    val sortingCode: String? = null
)
