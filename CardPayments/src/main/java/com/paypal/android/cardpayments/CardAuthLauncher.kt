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
        private const val METADATA_KEY_REQUEST_TYPE = "request_type"
        private const val REQUEST_TYPE_APPROVE_ORDER = "approve_order"
        private const val REQUEST_TYPE_VAULT = "vault"

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
                    .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_APPROVE_ORDER)
                    .put(METADATA_KEY_ORDER_ID, request.orderId)
            }

            is CardAuthChallenge.Vault -> {
                val request = authChallenge.request
                JSONObject()
                    .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
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
            is BrowserSwitchFinalResult.Failure -> TODO("Return an unknown error")
            BrowserSwitchFinalResult.NoResult -> CardStatus.NoResult
        }

    private fun parseBrowserSwitchSuccessResult(result: BrowserSwitchFinalResult.Success): CardStatus =
        when (result.requestCode) {
            BrowserSwitchRequestCodes.CARD_APPROVE_ORDER -> parseApproveOrderSuccessResult(result)
            BrowserSwitchRequestCodes.CARD_VAULT -> parseVaultSuccessResult(result)
            else -> CardStatus.NoResult
        }

    private fun parseVaultSuccessResult(finalResult: BrowserSwitchFinalResult.Success): CardStatus {
        val deepLinkUrl = finalResult.returnUrl
        val requestMetadata = finalResult.requestMetadata

        return if (requestMetadata == null) {
            CardStatus.VaultError(CardError.unknownError)
        } else {
            // TODO: see if there's a way that we can require the merchant to make their
            // return and cancel urls conform to a strict schema

            // NOTE: this assumes that when the merchant created a setup token, they used a
            // return_url with word "success" in it (or a cancel_url with the word "cancel" in it)
            val setupTokenId =
                finalResult.requestMetadata?.optString(METADATA_KEY_SETUP_TOKEN_ID)
            val deepLinkUrlString = deepLinkUrl.toString()
            val didSucceed = deepLinkUrlString.contains("success")
            if (didSucceed) {
                val result = CardVaultResult(setupTokenId!!, "SCA_COMPLETE")
                CardStatus.VaultSuccess(result)
            } else {
                val didCancel = deepLinkUrlString.contains("cancel")
                if (didCancel) {
                    CardStatus.VaultCanceled(setupTokenId)
                } else {
                    CardStatus.VaultError(CardError.unknownError)
                }
            }
        }
    }

    private fun parseApproveOrderSuccessResult(
        finalResult: BrowserSwitchFinalResult.Success,
    ): CardStatus {
        val deepLinkUrl = finalResult.returnUrl
        val orderId = finalResult.requestMetadata?.optString(METADATA_KEY_ORDER_ID)

        return if (orderId == null) {
            CardStatus.ApproveOrderError(CardError.unknownError, orderId)
        } else if (deepLinkUrl.getQueryParameter("error") != null) {
            CardStatus.ApproveOrderError(CardError.threeDSVerificationError, orderId)
        } else {
            val state = deepLinkUrl.getQueryParameter("state")
            val code = deepLinkUrl.getQueryParameter("code")
            if (state == null || code == null) {
                CardStatus.ApproveOrderError(CardError.malformedDeepLinkError, orderId)
            } else {
                val liabilityShift = deepLinkUrl.getQueryParameter("liability_shift")
                val result = CardResult(
                    orderId = orderId,
                    liabilityShift = liabilityShift,
                    didAttemptThreeDSecureAuthentication = true
                )
                CardStatus.ApproveOrderSuccess(result)
            }
        }
    }
}
