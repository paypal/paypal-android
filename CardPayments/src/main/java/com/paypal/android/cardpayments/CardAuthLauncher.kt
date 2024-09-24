package com.paypal.android.cardpayments

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchStartResult
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
        authChallenge: CardAuthChallenge
    ): CardAuthChallengeResult {
        val analytics = authChallenge.analytics
        return when (val result = browserSwitchClient.start(activity, authChallenge.options)) {
            is BrowserSwitchStartResult.Failure -> {
                analytics.notify3DSFailed()
                val message = "auth challenge failed"
                CardAuthChallengeResult.Failure(PayPalSDKError(123, message, reason = result.error))
            }

            is BrowserSwitchStartResult.Started -> {
                analytics.notify3DSSucceeded()
                CardAuthChallengeResult.Success(result.pendingRequest)
            }
        }
    }

    fun checkIfApproveOrderAuthComplete(
        intent: Intent,
        state: String
    ): CardApproveOrderAuthResult =
        when (val result = browserSwitchClient.completeRequest(intent, state)) {
            is BrowserSwitchFinalResult.Success -> {
                val requestType = result.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_APPROVE_ORDER) {
                    val orderId = result.requestMetadata?.optString(METADATA_KEY_ORDER_ID)
                    parseApproveOrderSuccessResult(result, orderId)
                } else {
                    CardApproveOrderAuthResult.NoResult
                }
            }

            is BrowserSwitchFinalResult.Failure -> CardApproveOrderAuthResult.Failure(
                PayPalSDKError(123, "broser switch error", reason = result.error)
            )

            BrowserSwitchFinalResult.NoResult -> CardApproveOrderAuthResult.NoResult
        }

    fun checkIfVaultAuthComplete(intent: Intent, state: String): CardVaultAuthResult =
        when (val result = browserSwitchClient.completeRequest(intent, state)) {
            is BrowserSwitchFinalResult.Success -> {
                val requestType = result.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_VAULT) {
                    parseVaultSuccessResult(result)
                } else {
                    CardVaultAuthResult.NoResult
                }
            }

            is BrowserSwitchFinalResult.Failure -> CardVaultAuthResult.Failure(
                PayPalSDKError(123, "browser switch error", reason = result.error)
            )

            BrowserSwitchFinalResult.NoResult -> CardVaultAuthResult.NoResult
        }

    private fun parseVaultSuccessResult(browserSwitchResult: BrowserSwitchFinalResult.Success): CardVaultAuthResult {
        val deepLinkUrl = browserSwitchResult.returnUrl
        val requestMetadata = browserSwitchResult.requestMetadata

        return if (deepLinkUrl == null || requestMetadata == null) {
            CardVaultAuthResult.Failure(CardError.unknownError)
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
                CardVaultAuthResult.Success(setupTokenId!!, "SCA_COMPLETE")
            } else {
                val didCancel = deepLinkUrlString.contains("cancel")
                if (didCancel) {
                    CardVaultAuthResult.Failure(PayPalSDKError(123, "user canceled"))
                } else {
                    CardVaultAuthResult.Failure(CardError.unknownError)
                }
            }
        }
    }

    private fun parseApproveOrderSuccessResult(
        browserSwitchResult: BrowserSwitchFinalResult.Success,
        orderId: String?
    ): CardApproveOrderAuthResult {
        val deepLinkUrl = browserSwitchResult.returnUrl
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
                    orderId = orderId ?: TODO("figure out if order id is critical in the response"),
                    liabilityShift = liabilityShift,
                )
            }
        }
    }
}
