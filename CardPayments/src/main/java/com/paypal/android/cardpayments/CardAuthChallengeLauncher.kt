package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStatus

class CardAuthChallengeLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient
) {
    constructor() : this(BrowserSwitchClient())

    fun parseResult(context: Context, intent: Intent?): CardAuthChallengeResult? =
        browserSwitchClient.parseResult(context, 123, intent)?.let { browserSwitchResult ->
            val approveOrderMetadata =
                ApproveOrderMetadata.fromJSON(browserSwitchResult.requestMetadata)

            if (approveOrderMetadata == null) {
                CardAuthChallengeError("invalid metadata")
            } else {
                when (browserSwitchResult.status) {
                    BrowserSwitchStatus.SUCCESS -> {
                        val deepLinkUrl = browserSwitchResult.deepLinkUrl
                        CardAuthChallengeSuccess(approveOrderMetadata, deepLinkUrl)
                    }

                    BrowserSwitchStatus.CANCELED ->
                        CardAuthChallengeError("user canceled", approveOrderMetadata.orderId)

                    else -> CardAuthChallengeError("unknown error", approveOrderMetadata.orderId)
                }
            }
        }

    fun clearResult(context: Context) {
        browserSwitchClient.clearActiveRequests(context)
    }

    fun launch(activity: FragmentActivity, authChallenge: CardAuthChallenge) {
        val urlScheme = authChallenge.returnUrl.scheme
        val approveOrderMetadata = authChallenge.approveOrderMetadata

        val options = BrowserSwitchOptions()
            .requestCode(123)
            .url(authChallenge.payerActionUri)
            .returnUrlScheme(urlScheme)
            .metadata(approveOrderMetadata.toJSON())

        browserSwitchClient.start(activity, options)
    }
}