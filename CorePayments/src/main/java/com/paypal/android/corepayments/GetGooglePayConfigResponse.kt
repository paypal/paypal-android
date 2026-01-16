package com.paypal.android.corepayments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetGooglePayConfigResponse(
    @SerialName("googlePayConfig")
    val googlePayConfig: GooglePayConfigData
)

@Serializable
internal data class GooglePayConfigData(
    val isEligible: Boolean,
    val apiVersion: Int? = null,
    val apiVersionMinor: Int? = null,
    val countryCode: String? = null,
    val allowedPaymentMethods: List<AllowedPaymentMethod>? = null,
    val merchantInfo: MerchantInfoData? = null
)

@Serializable
data class AllowedPaymentMethod(
    val type: String,
    val parameters: AllowedPaymentMethodParameters? = null,
    val tokenizationSpecification: TokenizationSpecification? = null
)

@Serializable
data class AllowedPaymentMethodParameters(
    val allowedAuthMethods: List<String>? = null,
    val allowedCardNetworks: List<String>? = null,
    val billingAddressRequired: Boolean? = null,
    val assuranceDetailsRequired: Boolean? = null,
    val billingAddressParameters: BillingAddressParameters? = null
)

@Serializable
data class BillingAddressParameters(
    val format: String? = null
)

@Serializable
data class TokenizationSpecification(
    val type: String,
    val parameters: TokenizationParameters? = null
)

@Serializable
data class TokenizationParameters(
    val gateway: String? = null,
    val gatewayMerchantId: String? = null
)

@Serializable
data class MerchantInfoData(
    val merchantOrigin: String? = null,
    val merchantId: String? = null
)
