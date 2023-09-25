package com.paypal.android.paypalnativepayments

/**
 * Used to configure options for approving a PayPal native order
 */
data class PayPalNativeCheckoutRequest(
    /**
     * The order ID associated with the request.
     */
    val orderId: String,

    /**
     * Optional: The setup configuration for authentication
     */
    val authConfig: AuthConfig? = null
)

/**
 * Used for setting up authentication configurations
 */
data class AuthConfig(
    /**
     * The email for preloading authentication
     */
    val userEmail: String? = null
)
