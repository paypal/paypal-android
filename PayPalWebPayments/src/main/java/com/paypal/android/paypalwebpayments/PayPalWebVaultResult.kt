package com.paypal.android.paypalwebpayments

/**
 * A result passed to a [PayPalWebVaultListener] when the PayPal flow completes successfully.
 * @property [approvalSessionId] session ID associated with vault approval by the PayPal account owner
 */
data class PayPalWebVaultResult internal constructor(val approvalSessionId: String)
