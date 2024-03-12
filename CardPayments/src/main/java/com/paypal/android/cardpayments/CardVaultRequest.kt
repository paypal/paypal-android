package com.paypal.android.cardpayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @suppress
 *
 * A vault request to attach a payment method to a setup token.
 *
 * @property setupTokenId id for the setup token to update.
 * @property card card payment source to attach to the setup token.
 * @property returnUrl return url for deep linking back into the merchant app after an auth challenge
 */
@Parcelize
data class CardVaultRequest(
    val setupTokenId: String,
    val card: Card,
    val returnUrl: String? = "",
) : Parcelable
