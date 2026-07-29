package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
 *
 * @property [setupTokenId] ID for the setup token associated with the vault approval
 * @property [returnToAppStrategy] Strategy for returning to the app after checkout flow
 */
@Deprecated(
    message = "Use createPayPalSession() followed by vault(activity, setupTokenId, callback) instead.",
    level = DeprecationLevel.WARNING
)
data class PayPalWebVaultRequest(
    val setupTokenId: String,
    val returnToAppStrategy: ReturnToAppStrategy? = null
)
