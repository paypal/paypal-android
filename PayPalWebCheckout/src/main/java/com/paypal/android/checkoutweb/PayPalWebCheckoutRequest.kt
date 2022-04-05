package com.paypal.android.checkoutweb

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderID The ID of the order to be approved.
 * @param funding specify funding (credit, paylater or default)
 */
data class PayPalWebCheckoutRequest @JvmOverloads constructor (val orderID: String, val funding: PayPalWebCheckoutFunding = PayPalWebCheckoutFunding.DEFAULT)
