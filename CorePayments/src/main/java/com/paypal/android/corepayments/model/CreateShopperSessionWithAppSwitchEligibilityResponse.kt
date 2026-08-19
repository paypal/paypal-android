package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CreateShopperSessionWithAppSwitchEligibilityResponse(
    val appSwitchEligible: Boolean,
    val redirectUrl: String,
    val checkoutFallbackUrl: String,
    val ineligibleReason: String?,
    val matchedAuthenticationMethods: List<String>,
    val shopperSessionConfig: ShopperSessionConfig
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ShopperSessionConfig(
    val id: String,
    val expiresAt: String
)
