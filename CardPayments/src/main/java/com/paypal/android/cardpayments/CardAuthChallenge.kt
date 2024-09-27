package com.paypal.android.cardpayments

import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions

/**
 * Pass this object to [CardClient.presentAuthChallenge] to present an authentication challenge
 * that was received in response to a [CardClient.approveOrder] or [CardClient.vault] call.
 */
data class CardAuthChallenge(
    val options: BrowserSwitchOptions
)
