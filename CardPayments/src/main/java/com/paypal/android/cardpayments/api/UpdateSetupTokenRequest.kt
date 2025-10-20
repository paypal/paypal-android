package com.paypal.android.cardpayments.api

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data classes for Kotlin serialization of update vault setup token GraphQL request
 */
@InternalSerializationApi
@Serializable
internal data class UpdateSetupTokenVariables(
    @SerialName("clientId")
    val clientId: String,
    @SerialName("vaultSetupToken")
    val vaultSetupToken: String,
    @SerialName("paymentSource")
    val paymentSource: VaultPaymentSource
)

@InternalSerializationApi
@Serializable
internal data class VaultPaymentSource(
    val card: VaultCard
)

@InternalSerializationApi
@Serializable
internal data class VaultCard(
    val number: String,
    val expiry: String,
    val name: String? = null,
    @SerialName("securityCode")
    val securityCode: String,
    @SerialName("billingAddress")
    val billingAddress: VaultBillingAddress? = null
)

@InternalSerializationApi
@Serializable
internal data class VaultBillingAddress(
    @SerialName("addressLine1")
    val addressLine1: String? = null,
    @SerialName("addressLine2")
    val addressLine2: String? = null,
    @SerialName("adminArea1")
    val adminArea1: String? = null,
    @SerialName("adminArea2")
    val adminArea2: String? = null,
    @SerialName("postalCode")
    val postalCode: String? = null,
    @SerialName("countryCode")
    val countryCode: String
)

/**
 * Data classes for update vault setup token GraphQL response
 */
@InternalSerializationApi
@Serializable
internal data class UpdateSetupTokenResponse(
    @SerialName("updateVaultSetupToken")
    val updateVaultSetupToken: SetupTokenData
)

@InternalSerializationApi
@Serializable
internal data class SetupTokenData(
    val id: String,
    val status: String,
    val links: List<LinkData>? = null
)

@InternalSerializationApi
@Serializable
internal data class LinkData(
    val rel: String,
    val href: String
)
