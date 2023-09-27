package com.paypal.android.cardpayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @suppress
 *
 * A result returned by [CardClient] when an a successful vault occurs.
 *
 * @param setupTokenId the id for the setup token that was recently updated
 * @param status the status of the updated setup token
 */
@Parcelize
data class CardVaultResult(
    val setupTokenId: String,
    val status: String
) : Parcelable
