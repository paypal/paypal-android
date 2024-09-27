package com.paypal.android.cardpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchLaunchResult
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchRequestCode
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStatus

internal class CardAuthLauncher constructor(
    private val analytics: CardAnalytics,
    private val browserSwitchClient: BrowserSwitchClient
) {

    // fully encapsulate browser switch
    constructor(analytics: CardAnalytics) : this(analytics, BrowserSwitchClient())

    fun createAuthChallenge(
        cardRequest: CardApproveOrderRequest,
        challengeUrl: String
    ): CardAuthChallenge {
        val metadata = cardRequest.run { CardAuthMetadata.ApproveOrder(config, orderId) }
        val options = BrowserSwitchOptions(
            code = BrowserSwitchRequestCode.CARD_APPROVE_ORDER,
            urlToOpen = Uri.parse(challengeUrl),
            returnUrl = Uri.parse(cardRequest.returnUrl),
            metadata = metadata.encodeToString()
        )
        return CardAuthChallenge(options)
    }

    fun createAuthChallenge(
        cardVaultRequest: CardVaultRequest,
        challengeUrl: String
    ): CardAuthChallenge {
        val metadata = cardVaultRequest.run { CardAuthMetadata.ApproveOrder(config, setupTokenId) }
        val options = BrowserSwitchOptions(
            code = BrowserSwitchRequestCode.CARD_VAULT,
            urlToOpen = Uri.parse(challengeUrl),
            returnUrl = Uri.parse(cardVaultRequest.returnUrl),
            metadata = metadata.encodeToString()
        )
        return CardAuthChallenge(options)
    }

    fun presentAuthChallenge(
        activity: FragmentActivity,
        authChallenge: CardAuthChallenge
    ): CardAuthChallengeResult {
        val analytics = analytics.createAnalyticsContext()
        return when (val result = browserSwitchClient.launch(activity, authChallenge.options)) {
            BrowserSwitchLaunchResult.Success -> {
                analytics.notify3DSSucceeded()
                CardAuthChallengeResult.Success(authChallenge.options.encodeToString())
            }

            is BrowserSwitchLaunchResult.Failure -> {
                analytics.notify3DSFailed()
                val error = PayPalSDKError(123, "auth challenge failed", reason = result.error)
                CardAuthChallengeResult.Failure(error)
            }
        }
    }

    fun checkIfApproveOrderAuthComplete(intent: Intent, state: String): CardApproveOrderAuthResult {
        val requestCode = BrowserSwitchRequestCode.CARD_APPROVE_ORDER
        val options = BrowserSwitchOptions.decodeIfRequestCodeMatches(state, requestCode)
            ?: return CardApproveOrderAuthResult.NoResult

        return when (val metadata = CardAuthMetadata.decodeFromString(options.metadata)) {
            is CardAuthMetadata.ApproveOrder -> {
                when (val status = browserSwitchClient.parseStatus(intent, options)) {
                    is BrowserSwitchStatus.Complete -> parseApproveOrderAuthResult(status, metadata)
                    else -> CardApproveOrderAuthResult.NoResult
                }
            }

            else -> CardApproveOrderAuthResult.NoResult
        }
    }

    fun checkIfVaultAuthComplete(intent: Intent, state: String): CardVaultAuthResult {
        val requestCode = BrowserSwitchRequestCode.CARD_VAULT
        val status = browserSwitchClient.parseStatus(intent, requestCode, state)
            ?: return CardVaultAuthResult.NoResult

        return when (status) {
            is BrowserSwitchStatus.Complete -> parseVaultAuthResult(status)
            is BrowserSwitchStatus.NoResult -> CardVaultAuthResult.NoResult
        }
    }

    private fun parseVaultAuthResult(status: BrowserSwitchStatus.Complete): CardVaultAuthResult {
        val metadata =
            CardAuthMetadata.decodeFromString(status.options.metadata) as? CardAuthMetadata.Vault
                ?: return CardVaultAuthResult.Failure(CardError.unknownError)

        val deepLinkUrl = status.deepLinkUri

        // TODO: see if there's a way that we can require the merchant to make their
        // return and cancel urls conform to a strict schema

        // NOTE: this assumes that when the merchant created a setup token, they used a
        // return_url with word "success" in it (or a cancel_url with the word "cancel" in it)
        val deepLinkUrlString = deepLinkUrl.toString()
        val didSucceed = deepLinkUrlString.contains("success")
        return if (didSucceed) {
            CardVaultAuthResult.Success(metadata.setupTokenId, "SCA_COMPLETE")
        } else {
            val didCancel = deepLinkUrlString.contains("cancel")
            if (didCancel) {
                CardVaultAuthResult.Failure(PayPalSDKError(123, "user canceled"))
            } else {
                CardVaultAuthResult.Failure(CardError.unknownError)
            }
        }
    }

    private fun parseApproveOrderAuthResult(
        status: BrowserSwitchStatus.Complete,
        metadata: CardAuthMetadata.ApproveOrder
    ): CardApproveOrderAuthResult {
        val deepLinkUrl = status.deepLinkUri
        val orderId = metadata.orderId

        return if (deepLinkUrl.getQueryParameter("error") != null) {
            CardApproveOrderAuthResult.Failure(CardError.threeDSVerificationError, orderId)
        } else {
            val state = deepLinkUrl.getQueryParameter("state")
            val code = deepLinkUrl.getQueryParameter("code")
            if (state == null || code == null) {
                CardApproveOrderAuthResult.Failure(CardError.malformedDeepLinkError, orderId)
            } else {
                val liabilityShift = deepLinkUrl.getQueryParameter("liability_shift")
                CardApproveOrderAuthResult.Success(
                    orderId = orderId,
                    liabilityShift = liabilityShift,
                )
            }
        }
    }
}
