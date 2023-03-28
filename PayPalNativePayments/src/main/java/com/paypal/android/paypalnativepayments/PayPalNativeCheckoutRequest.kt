package com.paypal.android.paypalnativepayments

/**
 * Used to configure options for approving a PayPal native order
 */
data class PayPalNativeCheckoutRequest(
    /**
     * The order ID associated with the request.
     */
    val orderID: String
)
