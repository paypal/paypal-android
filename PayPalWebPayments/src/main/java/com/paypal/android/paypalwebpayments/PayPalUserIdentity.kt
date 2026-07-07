package com.paypal.android.paypalwebpayments

/**
 * Represents the identity of the shopper initiating a PayPal session.
 *
 * Pass one of the subclasses to [PayPalWebCheckoutClient.createPayPalSession] to pre-warm
 * the shopper session with the appropriate identity signal.
 */
sealed class PayPalUserIdentity {

    /**
     * Identifies the shopper via a server-side shopper session id.
     *
     * @param serverSideShopperSessionId The session id obtained from the merchant's server.
     */
    data class ServerSideShopperSession(
        val serverSideShopperSessionId: String
    ) : PayPalUserIdentity()

    /**
     * Identifies the shopper via email and/or phone number.
     *
     * @param email The shopper's email address, or null if not available.
     * @param phone The shopper's phone number, or null if not available.
     */
    data class Email(
        val email: String? = null,
        val phone: String? = null
    ) : PayPalUserIdentity()

    /**
     * No identity signal is provided. The shopper session is created without pre-identification.
     */
    data object None : PayPalUserIdentity()
}
