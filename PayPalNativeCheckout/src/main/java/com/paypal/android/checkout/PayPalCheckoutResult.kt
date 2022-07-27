package com.paypal.android.checkout

import com.paypal.android.checkout.model.buyer.Buyer


/**
 * A result passed to a [PayPalListener] when the PayPal flow completes successfully.
 */
data class PayPalCheckoutResult(
    val orderId: String?,
    val payerId: String?,
    val payer: Buyer? = null,
    val billingToken: String? = null
)
