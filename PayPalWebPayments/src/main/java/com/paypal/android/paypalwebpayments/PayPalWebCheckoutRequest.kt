package com.paypal.android.paypalwebpayments

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderId The ID of the order to be approved.
 * @param fundingSource specify funding (credit, paylater or default)
 */
data class PayPalWebCheckoutRequest @JvmOverloads constructor(
    val orderId: String,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL
)
