package com.paypal.android.paypalwebpayments

/**
 * Request to start a PayPal web checkout flow with [PayPalWebCheckoutClient.start].
 *
 * @property userIdentity Buyer identity information. Use [PayPalUserIdentity.Unknown] to opt out.
 * @property payPalURLConfig URLs used to return the buyer to the merchant app after checkout.
 * @property userAction Controls the call-to-action label on the PayPal checkout page.
 * @param fundingSource specify funding (credit, paylater or default)
 */
data class PayPalWebCheckoutRequest(
    val userIdentity: PayPalUserIdentity,
    val payPalURLConfig: PayPalURLConfig,
    val userAction: PayPalUserAction = PayPalUserAction.CONTINUE,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL,
)
