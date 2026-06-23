package com.paypal.android.paypalwebpayments

/**
 * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
 *
 * @property userIdentity Buyer identity information. Use [PayPalUserIdentity.Unknown] to opt out.
 * @property returnToAppUrlConfig URLs used to return the buyer to the merchant app after vaulting.
 * @property userAction Controls the call-to-action label on the PayPal vault page.
 */
data class PayPalWebVaultRequest(
    val userIdentity: PayPalUserIdentity,
    val returnToAppUrlConfig: ReturnToAppUrlConfig,
    val userAction: PayPalUserAction = PayPalUserAction.CONTINUE
)
