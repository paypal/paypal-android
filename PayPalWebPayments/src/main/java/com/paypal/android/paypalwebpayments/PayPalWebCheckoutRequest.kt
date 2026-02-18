package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Creates an instance of a PayPalRequest.
 *
 * @param orderId The ID of the order to be approved.
 * @param fundingSource specify funding (credit, paylater or default)
 * @param appSwitchWhenEligible whether to switch to the PayPal app when eligible
 * @param returnToAppStrategy Strategy for returning to the app after checkout flow
 */
data class PayPalWebCheckoutRequest @JvmOverloads constructor(
    val orderId: String,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL,
    val appSwitchWhenEligible: Boolean = false,
    val returnToAppStrategy: ReturnToAppStrategy? = null
)
