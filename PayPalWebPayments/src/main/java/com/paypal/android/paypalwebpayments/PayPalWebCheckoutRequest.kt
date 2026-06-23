package com.paypal.android.paypalwebpayments

/**
 * Request to start a PayPal web checkout flow with [PayPalWebCheckoutClient.start].
 *
 * @property userIdentity Buyer identity information. Use [PayPalUserIdentity.Unknown] to opt out.
 * @property returnToAppUrlConfig URLs used to return the buyer to the merchant app after checkout.
 * @property userAction Controls the call-to-action label on the PayPal checkout page.
 */
data class PayPalWebCheckoutRequest(
    val userIdentity: PayPalUserIdentity,
    val returnToAppUrlConfig: ReturnToAppUrlConfig,
    val userAction: PayPalUserAction = PayPalUserAction.CONTINUE
)
