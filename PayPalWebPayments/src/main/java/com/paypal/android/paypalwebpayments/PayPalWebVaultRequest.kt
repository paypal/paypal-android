package com.paypal.android.paypalwebpayments

/**
 * Request to vault a PayPal payment method using [PayPalWebCheckoutClient.vault].
 *
 * @property userIdentity Buyer identity (Shopper Session ID, email, phone, or [PayPalUserIdentity.Unknown]).
 * @property returnToAppUrlConfig URL configuration for app return / cancel handling.
 * @property userAction Controls the call-to-action label on the PayPal vault page.
 *   Defaults to [PayPalUserAction.CONTINUE].
 */
data class PayPalWebVaultRequest @JvmOverloads constructor(
    val userIdentity: PayPalUserIdentity,
    val returnToAppUrlConfig: ReturnToAppUrlConfig,
    val userAction: PayPalUserAction = PayPalUserAction.CONTINUE,
)
