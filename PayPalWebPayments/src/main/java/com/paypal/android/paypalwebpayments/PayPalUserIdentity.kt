package com.paypal.android.paypalwebpayments

/**
 * Represents the identity of the shopper initiating a PayPal session.
 *
 * Pass to [PayPalWebCheckoutClient.createPayPalSession] to pre-warm the shopper session
 * with the appropriate identity signal.
 *
 * @param existingPayPalSessionId A server-side shopper session id.
 * @param email The shopper's email address.
 * @param phone The shopper's phone number.
 */
data class PayPalUserIdentity(
    val existingPayPalSessionId: String? = null,
    val email: String? = null,
    val phone: String? = null,
)
