package com.paypal.android.cardpayments

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus

class CardAuthChallengeLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient
) {
    constructor() : this(BrowserSwitchClient())

    fun parseResult(context: Context, intent: Intent?): CardAuthChallengeResult? =
        browserSwitchClient.parseResult(context, 123, intent)?.let { browserSwitchResult ->
            when (browserSwitchResult.status) {
                BrowserSwitchStatus.SUCCESS -> parseAuthChallengeSuccessResult(browserSwitchResult)
                BrowserSwitchStatus.CANCELED -> CardAuthChallengeError("user canceled")
                else -> CardAuthChallengeError("unknown error")
            }
        }

    fun clearResult(context: Context) {
        browserSwitchClient.clearActiveRequests(context)
    }

    private fun parseAuthChallengeSuccessResult(browserSwitchResult: BrowserSwitchResult): CardAuthChallengeResult {
        ApproveOrderMetadata.fromJSON(browserSwitchResult.requestMetadata)
            ?.let { approveOrderMetadata ->
                return CardAuthChallengeSuccess(
                    approveOrderMetadata,
                    browserSwitchResult.deepLinkUrl
                )
            }
        return CardAuthChallengeError("invalid metadata")
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