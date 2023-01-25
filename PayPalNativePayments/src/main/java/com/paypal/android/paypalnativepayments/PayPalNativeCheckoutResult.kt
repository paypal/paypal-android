package com.paypal.android.paypalnativepayments

import com.paypal.checkout.approve.Approval

/**
 * A result passed to a [PayPalNativeCheckoutListener] when the PayPal flow completes successfully.
 */
data class PayPalNativeCheckoutResult(
    val approval: Approval,
)
