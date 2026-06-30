package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Request to initiate a PayPal checkout.
 *
 * @deprecated Use [PayPalWebCheckoutClient.startPayPalSession] followed by
 * [PayPalWebCheckoutClient.start] with only the order ID instead.
 */
@Deprecated(
    message = "Use startPayPalSession() followed by start(activity, orderId, callback) instead.",
    level = DeprecationLevel.WARNING
)
data class PayPalWebCheckoutRequest @JvmOverloads constructor(
    val orderId: String,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL,
    val returnToAppStrategy: ReturnToAppStrategy? = null
)
