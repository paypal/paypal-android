package com.paypal.android.paypalnativepayments

/**
 * Deprecated. Use PayPalWebPayments module instead
 * A result passed to a [PayPalNativeCheckoutListener] when the PayPal flow completes successfully.
 */
@Deprecated("Deprecated. Use PayPalWebPayments module instead")
data class PayPalNativeCheckoutResult(
    /**
     *  The order ID associated with the transaction
     */
    val orderId: String?,
    /**
     * The Payer ID (or user ID) associated with the transaction
     */
    val payerId: String?
)
