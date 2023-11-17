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
     * Optional: User email to initiate a quicker authentication flow
     * in cases where the user has a PayPal Account with the same email.
     */
    val userAuthenticationEmail: String? = null
)
