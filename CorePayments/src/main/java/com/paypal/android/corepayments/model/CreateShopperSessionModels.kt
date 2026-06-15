package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

// ── GraphQL Variables ─────────────────────────────────────────────────────────

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class CreateShopperSessionVariables(
    val bnCode: String?,
    val sdkVersion: String,
    val osVersion: String,
    val osType: String,
    val integrationChannel: String,
    val paymentMethodSelected: String,
    val flowType: String,
    val paymentType: String,
    val contextId: String,
    val tokenType: String?,
    val buyerEmailAddressMerchantPassed: String?,
    val paypalNativeAppInstalled: Boolean,
    val returnAppUrl: String,
    val cancelAppUrl: String,
    val fallbackSchemeUrl: String,
)

// ── GraphQL Response ──────────────────────────────────────────────────────────

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class CreateShopperSessionMutationResponse(
    val createShopperSessionWithAppSwitchEligibility: ShopperSessionWithEligibilityData? = null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class ShopperSessionWithEligibilityData(
    val appSwitchEligible: Boolean = false,
    val redirectURL: String? = null,
    val checkoutFallbackUrl: String? = null,
    val ineligibleReason: String? = null,
    val matchedAuthenticationMethods: List<String>? = null,
    val shopperSessionConfig: ShopperSessionConfigData? = null,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(InternalSerializationApi::class)
@Serializable
data class ShopperSessionConfigData(
    val id: String,
    val expiresAt: String,
)

// ── Clean Result Model ────────────────────────────────────────────────────────

/**
 * Result of a successful `createShopperSessionWithAppSwitchEligibility` call.
 *
 * @property appSwitchEligible Whether the PayPal app-switch flow is eligible for this session.
 * @property redirectURL App-switch URL. Only present when [appSwitchEligible] is `true`.
 * @property checkoutFallbackUrl Browser-based checkout URL. Always present when
 *   `integrationChannel = PPCP_NATIVE_SDK`.
 * @property ineligibleReason Reason code when not eligible. Logged for analytics.
 * @property shopperSessionId The created shopper session ID (SSID), appended to checkout URLs.
 * @property expiresAt The date / time the Shopper Session data is no longer valid.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ShopperSessionWithAppSwitchEligibility(
    val appSwitchEligible: Boolean,
    val redirectURL: String?,
    val checkoutFallbackUrl: String?,
    val ineligibleReason: String?,
    val shopperSessionId: String?,
    val expiresAt: String?
)
