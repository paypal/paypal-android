package com.paypal.android.paypalwebpayments

/**
 * Request to start a PayPal web checkout flow using [PayPalWebCheckoutClient.start].
 *
 * @property userIdentity Buyer identity (Shopper Session ID, email, phone, or [PayPalUserIdentity.Unknown]).
 * @property returnToAppUrlConfig URL configuration for app return / cancel handling.
 * @property userAction Controls the call-to-action label on the PayPal checkout page.
 *   Defaults to [PayPalUserAction.CONTINUE].
 */
data class PayPalWebCheckoutRequest @JvmOverloads constructor(
    val userIdentity: PayPalUserIdentity,
    val returnToAppUrlConfig: ReturnToAppUrlConfig,
    val userAction: PayPalUserAction = PayPalUserAction.CONTINUE,
)
