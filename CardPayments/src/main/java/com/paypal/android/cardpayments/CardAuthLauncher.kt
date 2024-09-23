package com.paypal.android.cardpayments

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchStartResult
import com.paypal.android.corepayments.BrowserSwitchAuthChallenge
import com.paypal.android.corepayments.PayPalSDKError

sealed class CardAuthChallengeResult {
    data class Success(val authState: String) : CardAuthChallengeResult()
    data class Failure(val error: PayPalSDKError) : CardAuthChallengeResult()
}

class CardAuthLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient
) {

    // needed to fully encapsulate browser switch dependency
    constructor() : this(BrowserSwitchClient())

    companion object {
        private const val METADATA_KEY_REQUEST_TYPE = "request_type"
        private const val REQUEST_TYPE_APPROVE_ORDER = "approve_order"
        private const val REQUEST_TYPE_VAULT = "vault"

        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"
    }

    fun presentAuthChallenge(
        activity: FragmentActivity,
        authChallenge: BrowserSwitchAuthChallenge
    ): CardAuthChallengeResult =
        when (val result = browserSwitchClient.start(activity, authChallenge.options)) {
            is BrowserSwitchStartResult.Failure -> CardAuthChallengeResult.Failure(
                PayPalSDKError(123, "auth challenge failed", reason = result.error)
            )

            is BrowserSwitchStartResult.Started -> CardAuthChallengeResult.Success(result.pendingRequest)
        }

    fun parseApproveOrderAuthResponse(
        intent: Intent,
        state: String
    ): CardApproveOrderAuthResponse =
        when (val result = browserSwitchClient.completeRequest(intent, state)) {
            is BrowserSwitchFinalResult.Success -> {
                val requestType = result.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_APPROVE_ORDER) {
                    val orderId = result.requestMetadata?.optString(METADATA_KEY_ORDER_ID)
                    parseApproveOrderSuccessResult(result, orderId)
                } else {
                    CardApproveOrderAuthResponse.NoResult
                }
            }

            is BrowserSwitchFinalResult.Failure -> CardApproveOrderAuthResponse.Failure(
                PayPalSDKError(123, "broser switch error", reason = result.error)
            )

            BrowserSwitchFinalResult.NoResult -> CardApproveOrderAuthResponse.NoResult
        }

    fun parseVaultAuthResponse(intent: Intent, state: String): CardVaultAuthResponse =
        when (val result = browserSwitchClient.completeRequest(intent, state)) {
            is BrowserSwitchFinalResult.Success -> {
                val requestType = result.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_VAULT) {
                    parseVaultSuccessResult(result)
                } else {
                    CardVaultAuthResponse.NoResult
                }
            }

            is BrowserSwitchFinalResult.Failure -> CardVaultAuthResponse.Failure(
                PayPalSDKError(123, "browser switch error", reason = result.error)
            )

            BrowserSwitchFinalResult.NoResult -> CardVaultAuthResponse.NoResult
        }

    private fun parseVaultSuccessResult(browserSwitchResult: BrowserSwitchFinalResult.Success): CardVaultAuthResponse {
        val deepLinkUrl = browserSwitchResult.returnUrl
        val requestMetadata = browserSwitchResult.requestMetadata

        return if (deepLinkUrl == null || requestMetadata == null) {
            CardVaultAuthResponse.Failure(CardError.unknownError)
        } else {
            // TODO: see if there's a way that we can require the merchant to make their
            // return and cancel urls conform to a strict schema

            // NOTE: this assumes that when the merchant created a setup token, they used a
            // return_url with word "success" in it (or a cancel_url with the word "cancel" in it)
            val setupTokenId =
                browserSwitchResult.requestMetadata?.optString(METADATA_KEY_SETUP_TOKEN_ID)
            val deepLinkUrlString = deepLinkUrl.toString()
            val didSucceed = deepLinkUrlString.contains("success")
            if (didSucceed) {
                CardVaultAuthResponse.Success(
                    CardVaultResult.Success(setupTokenId!!, "SCA_COMPLETE")
                )
            } else {
                val didCancel = deepLinkUrlString.contains("cancel")
                if (didCancel) {
                    CardVaultAuthResponse.Failure(PayPalSDKError(123, "user canceled"))
                } else {
                    CardVaultAuthResponse.Failure(CardError.unknownError)
                }
            }
        }
    }

    private fun parseApproveOrderSuccessResult(
        browserSwitchResult: BrowserSwitchFinalResult.Success,
        orderId: String?
    ): CardApproveOrderAuthResponse {
        val deepLinkUrl = browserSwitchResult.returnUrl
        return if (deepLinkUrl.getQueryParameter("error") != null) {
            CardApproveOrderAuthResponse.Failure(CardError.threeDSVerificationError, orderId)
        } else {
            val state = deepLinkUrl.getQueryParameter("state")
            val code = deepLinkUrl.getQueryParameter("code")
            if (state == null || code == null) {
                CardApproveOrderAuthResponse.Failure(CardError.malformedDeepLinkError, orderId)
            } else {
                val liabilityShift = deepLinkUrl.getQueryParameter("liability_shift")
                val result = CardApproveOrderResult.Success(
                    orderId = orderId ?: TODO("figure out if order id is critical in the response"),
                    liabilityShift = liabilityShift,
                    didAttemptThreeDSecureAuthentication = true
                )
                CardApproveOrderAuthResponse.Success(result)
            }
        }
    }
}
