package com.paypal.android.cardpayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VaultRequest(
    val setupTokenId: String,
    val card: Card,
) : Parcelable
