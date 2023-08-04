package com.paypal.android.cardpayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VaultResult(
    val setupTokenId: String,
    val status: String
): Parcelable
