package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo

// TODO: Make sure this model is aligned once API changes are ready.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CreateShopperSessionWithAppSwitchEligibilityResponse(
    val appSwitchEligible: Boolean,
    val redirectUrl: String,
    val checkoutFallbackUrl: String,
    val inEligibleReason: String, //TODO: Replace With AppSwitchInEligibleReason,
    val matchedAuthenticationMethods: List<String>,
    val shopperSessionConfig: ShopperSessionConfig
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ShopperSessionConfig(
    val id: String,
    val expiresAt: String
)