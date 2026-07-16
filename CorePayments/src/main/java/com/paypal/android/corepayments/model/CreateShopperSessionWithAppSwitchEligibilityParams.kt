package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo

/**
 * Bundles the app-switch return URLs and payment context needed to create a shopper session.
 *
 * @param returnAppUrl Deep-link URL to return to the app on success.
 * @param cancelAppUrl Deep-link URL to return to the app on cancellation.
 * @param fallbackSchemeUrl Custom URL scheme used as fallback.
 * @param paymentType GraphQL paymentType value (e.g. "CONTINUE", "PAY_NOW").
 * @param paypalNativeAppInstalled Whether the PayPal native app is installed.
 * @param countryCode Country calling code for the shopper's phone number (e.g. "1").
 * @param nationalNumber National (subscriber) number for the shopper's phone number.
 * @param buyerEmailAddressMerchantPassed The shopper's email address, as passed by the
 * merchant to identify user.
 * @param existingPayPalSessionId A server-side shopper session id from a previous session.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CreateShopperSessionWithAppSwitchEligibilityParams(
    val returnAppUrl: String,
    val cancelAppUrl: String,
    val fallbackSchemeUrl: String?,
    val paymentType: String,
    val paypalNativeAppInstalled: Boolean,
    val countryCode: String? = null,
    val nationalNumber: String? = null,
    val buyerEmailAddressMerchantPassed: String? = null,
    val existingPayPalSessionId: String? = null,
)
