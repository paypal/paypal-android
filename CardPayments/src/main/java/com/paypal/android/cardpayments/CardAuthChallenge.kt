package com.paypal.android.cardpayments

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize

/**
 * Pass this object to [CardClient.presentAuthChallenge] to present an authentication challenge
 * that was received in response to a [CardClient.approveOrder] or [CardClient.vault] call.
 */
sealed class CardAuthChallenge {
    // Ref: https://stackoverflow.com/a/44420084
    internal abstract val url: Uri
    internal abstract val returnUrlScheme: String?
    internal abstract val appLinkUrl: String?

    @Parcelize
    internal class ApproveOrder(
        override val url: Uri,
        val request: CardRequest,
        override val returnUrlScheme: String? = extractCustomUrlScheme(request.returnUrl),
        override val appLinkUrl: String? = if (returnUrlScheme == null) request.returnUrl else null
    ) : CardAuthChallenge(), Parcelable

    @Parcelize
    internal class Vault(
        override val url: Uri,
        val request: CardVaultRequest,
        override val returnUrlScheme: String? = extractCustomUrlScheme(request.returnUrl),
        override val appLinkUrl: String? = if (returnUrlScheme == null) request.returnUrl else null
    ) : CardAuthChallenge(), Parcelable

    private companion object {
        val httpOrHttpsRegex = """^https?$""".toRegex()
        fun extractCustomUrlScheme(url: String?): String? {
            val scheme = url?.toUri()?.scheme
            return if (scheme == null || httpOrHttpsRegex.containsMatchIn(scheme)) {
                null
            } else {
                scheme
            }
        }
    }
}
