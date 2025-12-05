package com.paypal.android.cardpayments

import android.app.Activity
import android.content.Intent
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchFinishResult
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import org.json.JSONObject

internal class CardAuthLauncher(
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
) {

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
            appLinkUrl = null,
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
        val pendingState = BrowserSwitchPendingState.fromBase64(authState)
        return if (pendingState == null) {
            val invalidAuthStateError = PayPalSDKError(0, "Auth State Invalid.")
            CardFinishApproveOrderResult.Failure(invalidAuthStateError)
        } else {
            val requestCode = BrowserSwitchRequestCodes.CARD_APPROVE_ORDER
            when (val finalResult = pendingState.match(intent, requestCode)) {
                is BrowserSwitchFinishResult.Success -> parseApproveOrderSuccessResult(finalResult)
                is BrowserSwitchFinishResult.DeepLinkNotPresent,
                is BrowserSwitchFinishResult.DeepLinkDoesNotMatch,
                is BrowserSwitchFinishResult.RequestCodeDoesNotMatch -> CardFinishApproveOrderResult.NoResult
            }
        }
    }

    fun completeVaultAuthRequest(intent: Intent, authState: String): CardFinishVaultResult {
        val pendingState = BrowserSwitchPendingState.fromBase64(authState)
        return if (pendingState == null) {
            val invalidAuthStateError = PayPalSDKError(0, "Auth State Invalid.")
            CardFinishVaultResult.Failure(invalidAuthStateError)
        } else {
            val requestCode = BrowserSwitchRequestCodes.CARD_VAULT
            when (val finalResult = pendingState.match(intent, requestCode)) {
                is BrowserSwitchFinishResult.Success -> parseVaultSuccessResult(finalResult)
                is BrowserSwitchFinishResult.DeepLinkNotPresent,
                is BrowserSwitchFinishResult.DeepLinkDoesNotMatch,
                is BrowserSwitchFinishResult.RequestCodeDoesNotMatch -> CardFinishVaultResult.NoResult
            }
        }
    }

    private fun parseVaultSuccessResult(result: BrowserSwitchFinishResult.Success): CardFinishVaultResult =
        if (result.requestCode == BrowserSwitchRequestCodes.CARD_VAULT) {
            val setupTokenId = result.requestMetadata?.optString(METADATA_KEY_SETUP_TOKEN_ID)
            if (setupTokenId == null) {
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
        } else {
            CardFinishVaultResult.NoResult
        }

    private fun parseApproveOrderSuccessResult(
        finalResult: BrowserSwitchFinishResult.Success
    ): CardFinishApproveOrderResult =
        if (finalResult.requestCode == BrowserSwitchRequestCodes.CARD_APPROVE_ORDER) {
            val orderId = finalResult.requestMetadata?.optString(METADATA_KEY_ORDER_ID)
            if (orderId == null) {
                CardFinishApproveOrderResult.Failure(CardError.unknownError)
            } else {
                CardFinishApproveOrderResult.Success(
                    orderId = orderId,
                    didAttemptThreeDSecureAuthentication = true
                )
            }
        } else {
            CardFinishApproveOrderResult.NoResult
        }
}
