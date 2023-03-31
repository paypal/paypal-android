package com.paypal.android.paypalnativepayments

/**
 * A result passed to a [PayPalNativeCheckoutListener] when the PayPal flow completes successfully.
 */
data class PayPalNativeCheckoutResult(
    /**
     *  The order ID associated with the transaction
     */
    val orderID: String?,
    /**
     * The Payer ID (or user ID) associated with the transaction
     */
    val payerID: String?
)
