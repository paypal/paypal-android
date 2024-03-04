package com.paypal.android.cardpayments

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class CardAuthChallenge() {
    // Ref: https://stackoverflow.com/a/44420084
    internal abstract val url: Uri
    internal abstract val returnUrlScheme: String?

    @Parcelize
    internal class ApproveOrder(
        override val url: Uri,
        val request: CardRequest,
        override val returnUrlScheme: String? = Uri.parse(request.returnUrl).scheme
    ) : CardAuthChallenge(), Parcelable

    @Parcelize
    internal class Vault(
        override val url: Uri,
        val request: CardVaultRequest,
        override val returnUrlScheme: String? = Uri.parse(request.returnUrl).scheme
    ) : CardAuthChallenge(), Parcelable
}
