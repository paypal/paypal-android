package com.paypal.android.cardpayments

import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.corepayments.BrowserSwitchAuthChallenge

/**
 * Pass this object to [CardClient.presentAuthChallenge] to present an authentication challenge
 * that was received in response to a [CardClient.approveOrder] or [CardClient.vault] call.
 */
data class CardAuthChallenge(
    override val options: BrowserSwitchOptions,
    internal val analytics: CardAnalyticsContext
) : BrowserSwitchAuthChallenge
