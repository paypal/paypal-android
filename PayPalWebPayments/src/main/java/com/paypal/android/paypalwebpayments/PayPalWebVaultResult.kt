package com.paypal.android.paypalwebpayments

/**
 * A result passed to a [PayPalWebVaultListener] when the PayPal flow completes successfully.
 */
data class PayPalWebVaultResult(val approvalSessionId: String)
