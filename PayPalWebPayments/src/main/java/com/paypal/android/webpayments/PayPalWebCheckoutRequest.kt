package com.paypal.android.webpayments

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderID The ID of the order to be approved.
 * @param fundingSource specify funding (credit, paylater or default)
 */
data class PayPalWebCheckoutRequest @JvmOverloads constructor (
    val orderID: String,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL
)
