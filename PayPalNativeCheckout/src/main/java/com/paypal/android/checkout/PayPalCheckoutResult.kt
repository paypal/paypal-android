package com.paypal.android.checkout

import com.paypal.checkout.approve.Approval


/**
 * A result passed to a [PayPalCheckoutListener] when the PayPal flow completes successfully.
 */
data class PayPalCheckoutResult(
    val approval: Approval,
)
