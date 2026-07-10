package com.paypal.android.corepayments.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

// ── Variables ────────────────────────────────────────────────────────────────

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionVariables(
    val appSwitchEligibilityInput: CreateShopperSessionAppSwitchEligibilityInput,
    val shopperSessionInput: CreateShopperSessionShopperSessionInput,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionAppSwitchEligibilityInput(
    val contextId: String,
    val experimentationContext: CreateShopperSessionExperimentationContext,
    val merchantOptInForAppSwitch: Boolean,
    val osType: String,
    val paypalNativeAppInstalled: Boolean,
    val tokenType: String,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionExperimentationContext(
    val appSwitchSupported: Boolean,
    val buyerGUID: String?,
    val merchantAccountId: String?,
    val merchantCountry: String?,
    val integrationChannel: String,
    val isWebLLSEligible: Boolean,
    val isWebView: Boolean,
    val paymentType: String,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionShopperSessionInput(
    val returnAppUrl: String,
    val cancelAppUrl: String,
    val fallbackUrlScheme: String?,
    val sdkVersion: String?,
)

// ── Response ─────────────────────────────────────────────────────────────────

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionGraphQLResponse(
    val external: CreateShopperSessionExternalData? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionExternalData(
    val createShopperSessionWithAppSwitchEligibility: CreateShopperSessionData? = null,
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
    val checkoutUrls: CreateShopperSessionCheckoutUrls? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionCheckoutUrls(
    val redirectURL: String? = null,
    val checkoutFallbackUrl: String? = null,
)

@OptIn(InternalSerializationApi::class)
@Serializable
internal data class CreateShopperSessionSessionData(
    val sessionId: String = "",
    val expiresAt: String? = null,
)
