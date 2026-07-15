package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderId The ID of the order to be approved.
 * @param fundingSource specify funding (credit, paylater or default)
 * @param returnToAppStrategy Strategy for returning to the app after checkout flow
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
