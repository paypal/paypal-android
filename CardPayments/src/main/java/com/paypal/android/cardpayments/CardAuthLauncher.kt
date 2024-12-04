package com.paypal.android.cardpayments

import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import org.json.JSONObject

internal class CardAuthLauncher(
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
) {

    companion object {
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"
    }

    fun presentAuthChallenge(
        activity: ComponentActivity,
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
        val browserSwitchOptions = BrowserSwitchOptions()
            .url(authChallenge.url)
            .requestCode(requestCode)
            .returnUrlScheme(authChallenge.returnUrlScheme)
            .metadata(metadata)

        return when (val startResult = browserSwitchClient.start(activity, browserSwitchOptions)) {
            is BrowserSwitchStartResult.Started -> {
                CardPresentAuthChallengeResult.Success(startResult.pendingRequest)
            }

            is BrowserSwitchStartResult.Failure -> {
                val error = CardError.browserSwitchError(startResult.error)
                CardPresentAuthChallengeResult.Failure(error)
            }
        }
    }

    fun completeAuthRequest(intent: Intent, authState: String): CardStatus =
        when (val finalResult = browserSwitchClient.completeRequest(intent, authState)) {
            is BrowserSwitchFinalResult.Success -> parseBrowserSwitchSuccessResult(finalResult)
            is BrowserSwitchFinalResult.Failure -> CardStatus.UnknownError(finalResult.error)
            BrowserSwitchFinalResult.NoResult -> CardStatus.NoResult
        }

    private fun parseBrowserSwitchSuccessResult(result: BrowserSwitchFinalResult.Success): CardStatus =
        when (result.requestCode) {
            BrowserSwitchRequestCodes.CARD_APPROVE_ORDER -> parseApproveOrderSuccessResult(result)
            BrowserSwitchRequestCodes.CARD_VAULT -> parseVaultSuccessResult(result)
            else -> CardStatus.NoResult
        }

    private fun parseVaultSuccessResult(finalResult: BrowserSwitchFinalResult.Success): CardStatus {
        val setupTokenId = finalResult.requestMetadata?.optString(METADATA_KEY_SETUP_TOKEN_ID)
        return if (setupTokenId == null) {
            CardStatus.VaultError(CardError.unknownError)
        } else {
            // TODO: see if there's a way that we can require the merchant to make their
            // return and cancel urls conform to a strict schema
            val result =
                CardVaultResult(setupTokenId, null, didAttemptThreeDSecureAuthentication = true)
            CardStatus.VaultSuccess(result)
        }
    }

    private fun parseApproveOrderSuccessResult(
        finalResult: BrowserSwitchFinalResult.Success,
    ): CardStatus {
        val orderId = finalResult.requestMetadata?.optString(METADATA_KEY_ORDER_ID)
        return if (orderId == null) {
            CardStatus.ApproveOrderError(CardError.unknownError, null)
        } else {
            val result = LegacyCardResult(orderId = orderId, didAttemptThreeDSecureAuthentication = true)
            CardStatus.ApproveOrderSuccess(result)
        }
    }
}
