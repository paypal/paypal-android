package com.paypal.android.cardpayments

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CaptureDeepLinkResult
import com.paypal.android.corepayments.DeepLink
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import com.paypal.android.corepayments.captureDeepLink
import org.json.JSONObject

internal class CardAuthLauncher(private val browserSwitchClient: BrowserSwitchClient) {

    constructor(context: Context) : this(BrowserSwitchClient(context))

    companion object {
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"
    }

    fun presentAuthChallenge(
        activity: Activity,
        authChallenge: CardAuthChallenge
    ): CardPresentAuthChallengeResult {
        val metadata = when (authChallenge) {
            is CardAuthChallenge.ApproveOrder -> {
                val request = authChallenge.request
                JSONObject()
                    .put(METADATA_KEY_ORDER_ID, request.orderId)
            }

            is CardAuthChallenge.Vault -> {
                val request = authChallenge.request
                JSONObject()
                    .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
            }
        }

        val requestCode = when (authChallenge) {
            is CardAuthChallenge.ApproveOrder -> BrowserSwitchRequestCodes.CARD_APPROVE_ORDER
            is CardAuthChallenge.Vault -> BrowserSwitchRequestCodes.CARD_VAULT
        }

        // launch the 3DS flow
        val browserSwitchOptions = BrowserSwitchOptions(
            targetUri = authChallenge.url,
            requestCode = requestCode,
            returnUrlScheme = authChallenge.returnUrlScheme,
            appLinkUrl = authChallenge.appLinkUrl,
            metadata = metadata
        )

        return when (val startResult = browserSwitchClient.start(activity, browserSwitchOptions)) {
            is BrowserSwitchStartResult.Success -> {
                val pendingState = BrowserSwitchPendingState(browserSwitchOptions)
                CardPresentAuthChallengeResult.Success(pendingState.toBase64EncodedJSON())
            }

            is BrowserSwitchStartResult.Failure -> {
                val error = CardError.browserSwitchError(startResult.error)
                CardPresentAuthChallengeResult.Failure(error)
            }
        }
    }

    fun completeApproveOrderAuthRequest(
        intent: Intent,
        authState: String
    ): CardFinishApproveOrderResult {
        val requestCode = BrowserSwitchRequestCodes.CARD_APPROVE_ORDER
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> parseApproveOrderSuccessResult(result.deepLink)
            is CaptureDeepLinkResult.Failure -> CardFinishApproveOrderResult.Failure(result.reason)
            is CaptureDeepLinkResult.Ignore -> CardFinishApproveOrderResult.NoResult
        }
    }

    fun completeVaultAuthRequest(intent: Intent, authState: String): CardFinishVaultResult {
        val requestCode = BrowserSwitchRequestCodes.CARD_VAULT
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> parseVaultSuccessResult(result.deepLink)
            is CaptureDeepLinkResult.Failure -> CardFinishVaultResult.Failure(result.reason)
            is CaptureDeepLinkResult.Ignore -> CardFinishVaultResult.NoResult
        }
    }

    private fun parseVaultSuccessResult(deepLink: DeepLink): CardFinishVaultResult {
        val originalOptions = deepLink.originalOptions
        val setupTokenId = originalOptions.metadata?.optString(METADATA_KEY_SETUP_TOKEN_ID)
        return if (setupTokenId == null) {
            CardFinishVaultResult.Failure(CardError.unknownError)
        } else {
            // TODO: see if there's a way that we can require the merchant to make their
            // return and cancel urls conform to a strict schema
            CardFinishVaultResult.Success(
                setupTokenId,
                null,
                didAttemptThreeDSecureAuthentication = true
            )
        }
    }

    private fun parseApproveOrderSuccessResult(deepLink: DeepLink): CardFinishApproveOrderResult {
        val originalOptions = deepLink.originalOptions
        val orderId = originalOptions.metadata?.optString(METADATA_KEY_ORDER_ID)
        return if (orderId == null) {
            CardFinishApproveOrderResult.Failure(CardError.unknownError)
        } else {
            CardFinishApproveOrderResult.Success(
                orderId = orderId,
                didAttemptThreeDSecureAuthentication = true
            )
        }
    }
}
