package com.paypal.android.paypalwebpayments

/**
 * Represents the buyer identity passed to the PayPal checkout or vault flow.
 *
 * Choose the subclass that matches how the merchant supplies identity:
 * - [ServerSideShopperSession] — merchant has already created a Shopper Session server-side.
 * - [Email] — pass the buyer's email address.
 * - [Phone] — pass the buyer's phone number.
 * - [Unknown] — no identity information available.
 */
sealed class PayPalUserIdentity {

    /**
     * Identity backed by a server-side Shopper Session ID.
     *
     * @property serverSideShopperSessionId Shopper Session ID created by the merchant's server.
     */
    data class ServerSideShopperSession(
        val serverSideShopperSessionId: String
    ) : PayPalUserIdentity()

    /**
     * Identity backed by the buyer's email address.
     *
     * @property email Buyer's email address.
     */
    data class Email(val email: String) : PayPalUserIdentity()

    /**
     * Identity backed by the buyer's phone number.
     *
     * @property phone Buyer's phone number.
     */
    data class Phone(val phone: String) : PayPalUserIdentity()

    /**
     * No buyer identity information is available.
     */
    data object Unknown : PayPalUserIdentity()
}
