package com.paypal.android.cardpayments

import android.os.Parcelable
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import kotlinx.parcelize.Parcelize

/**
 * Pass this object to [CardClient.presentAuthChallenge] to present an authentication challenge
 * that was received in response to a [CardClient.approveOrder] or [CardClient.vault] call.
 */
@Parcelize
data class CardAuthChallenge(
    val options: BrowserSwitchOptions
) : Parcelable
