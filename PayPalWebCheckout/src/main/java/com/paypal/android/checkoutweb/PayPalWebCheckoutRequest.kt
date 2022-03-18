package com.paypal.android.checkoutweb

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderID The ID of the order to be approved.
 */
data class PayPalWebCheckoutRequest(val orderID: String)
