package com.paypal.android.cardpayments

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardAuthChallenge(
    internal val url: Uri,
    internal val request: CardVaultRequest,
) : Parcelable
