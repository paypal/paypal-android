package com.paypal.android.corepayments.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionVariables(
    val osType: String,
    val osVersion: String,
    val token: String,
    val tokenType: String,
    val contextId: String,
    val buyerEmailAddressMerchantPassed: String?,
    val paypalNativeAppInstalled: Boolean,
    val bnCode: String?,
    val integrationChannel: String?,
    val paymentMethodSelected: String?,
    val productCode: String?,
    val paymentType: String?,
    val returnAppUrl: String,
    val cancelAppUrl: String,
    val fallbackUrlScheme: String?,
    val sdkVersion: String?,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionGraphQLResponse(
    val createShopperSessionWithAppSwitchEligibility: CreateShopperSessionData? = null
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionData(
    val appSwitchEligibilityResponse: CreateShopperSessionAppSwitchData? = null,
    val shopperSessionResponse: CreateShopperSessionSessionData? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionAppSwitchData(
    val appSwitchEligible: Boolean = false,
    val ineligibleReason: String? = null,
    val redirectURL: String? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionSessionData(
    val sessionId: String = "",
    val expiresAt: String = "",
)
