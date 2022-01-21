package com.paypal.android.checkout


/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderID The ID of the order to be approved.
 */
data class PayPalRequest(val orderID: String)
