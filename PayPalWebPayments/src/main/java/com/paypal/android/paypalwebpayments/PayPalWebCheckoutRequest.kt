package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Creates an instance of a PayPalRequest.
 *
 * @deprecated Use [PayPalWebCheckoutClient.createPayPalSession] followed by
 * [PayPalWebCheckoutClient.start] with only the order id instead.
 */
@Deprecated(
    message = "Use createPayPalSession() followed by start(activity, orderId, callback) instead.",
    level = DeprecationLevel.WARNING
)
data class PayPalWebCheckoutRequest @JvmOverloads constructor(
    val orderId: String,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL,
    val returnToAppStrategy: ReturnToAppStrategy? = null
)
