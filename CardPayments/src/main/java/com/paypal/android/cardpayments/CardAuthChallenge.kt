package com.paypal.android.cardpayments

import android.net.Uri
import android.os.Parcelable
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.corepayments.BrowserSwitchAuthChallenge
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Pass this object to [CardClient.presentAuthChallenge] to present an authentication challenge
 * that was received in response to a [CardClient.approveOrder] or [CardClient.vault] call.
 */
sealed class CardAuthChallenge : BrowserSwitchAuthChallenge {

    companion object {
        private const val METADATA_KEY_REQUEST_TYPE = "request_type"
        private const val REQUEST_TYPE_APPROVE_ORDER = "approve_order"
        private const val REQUEST_TYPE_VAULT = "vault"

        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"

        fun create(cardRequest: CardApproveOrderRequest, authChallengeUrl: String): CardAuthChallenge {
            val returnUrlScheme: String? = Uri.parse(cardRequest.returnUrl).scheme
            return ApproveOrder(
                url = Uri.parse(authChallengeUrl),
                request = cardRequest,
                returnUrlScheme = returnUrlScheme
            )
        }

        fun create(
            cardVaultRequest: CardVaultRequest,
            authChallengeUrl: String
        ): CardAuthChallenge {
            val returnUrlScheme: String? = Uri.parse(cardVaultRequest.returnUrl).scheme
            return Vault(
                url = Uri.parse(authChallengeUrl),
                request = cardVaultRequest,
                returnUrlScheme = returnUrlScheme
            )
        }
    }

    // Ref: https://stackoverflow.com/a/44420084
    internal abstract val url: Uri
    internal abstract val returnUrlScheme: String?

    @Parcelize
    internal class ApproveOrder(
        override val url: Uri,
        val request: CardApproveOrderRequest,
        override val returnUrlScheme: String? = Uri.parse(request.returnUrl).scheme
    ) : CardAuthChallenge(), Parcelable {
        override val options: BrowserSwitchOptions

        init {
            val metadata = JSONObject()
                .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_APPROVE_ORDER)
                .put(METADATA_KEY_ORDER_ID, request.orderId)

            // launch the 3DS flow
            options = BrowserSwitchOptions()
                .url(url)
                .returnUrlScheme(returnUrlScheme)
                .metadata(metadata)
        }
    }

    @Parcelize
    internal class Vault(
        override val url: Uri,
        val request: CardVaultRequest,
        override val returnUrlScheme: String? = Uri.parse(request.returnUrl).scheme
    ) : CardAuthChallenge(), Parcelable {
        override val options: BrowserSwitchOptions

        init {
            val metadata = JSONObject()
                .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
                .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)

            // launch the 3DS flow
            options = BrowserSwitchOptions()
                .url(url)
                .returnUrlScheme(returnUrlScheme)
                .metadata(metadata)
        }
    }
}
