package com.paypal.android.paypalwebpayments

/**
 * Represents buyer identity information passed to the SDK.
 *
 * Use [ServerSideShopperSession] when the merchant has already created a shopper session
 * server-side and wants to pass the session ID directly.
 *
 * Use [Email] or [Phone] to pass hashed buyer identity for shopper session creation.
 *
 * Use [Unknown] to opt out of providing identity information.
 */
sealed class PayPalUserIdentity {
    /**
     * Buyer identifier
     */
    abstract val identifier: String?

    /**
     * Buyer identity via a server-side shopper session ID.
     *
     * @property serverSideShopperSessionId Session ID created by the merchant's server.
     */
    data class ServerSideShopperSession(
        val serverSideShopperSessionId: String,
    ) : PayPalUserIdentity() {
        override val identifier: String = serverSideShopperSessionId
    }

    /**
     * Buyer identity via email address.
     *
     * @property email The buyer's email address.
     */
    data class Email(val email: String) : PayPalUserIdentity() {
        override val identifier: String = email
    }

    /**
     * Buyer identity via phone number.
     *
     * @property phone The buyer's phone number.
     */
    data class Phone(val phone: String) : PayPalUserIdentity() {
        override val identifier: String? = null
    }

    /**
     * No buyer identity provided.
     */
    data object Unknown : PayPalUserIdentity() {
        override val identifier: String? = null
    }
}
